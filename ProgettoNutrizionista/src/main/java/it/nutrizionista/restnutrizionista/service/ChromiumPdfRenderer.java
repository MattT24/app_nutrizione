package it.nutrizionista.restnutrizionista.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import it.nutrizionista.restnutrizionista.exception.PdfGenerationException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rendering HTML → PDF via Chromium headless (Playwright). Sostituisce il precedente motore
 * OpenHTMLToPDF: dal punto di vista del chiamante {@link #render(String)} resta una chiamata
 * <b>bloccante</b> — nessuna asincronia viene introdotta sul path richiesta→PDF→email, apposta per
 * non rompere la propagazione di {@code SecurityContext}/audit (vedi {@code PdfShareService}).
 * Internamente il lavoro viene delegato a un pool di worker thread dedicati.
 *
 * <p>Ogni worker possiede la propria istanza {@link Playwright}/{@link Browser}: un'istanza
 * Playwright <b>non è thread-safe</b> e va usata solo dal thread che l'ha creata, quindi il pool
 * non usa un {@code ThreadPoolTaskExecutor} generico con {@code CallerRunsPolicy} (il thread
 * chiamante non possiede un browser proprio) ma worker thread-confinati, ciascuno col proprio
 * {@link ExecutorService} single-thread.
 */
@Component
public class ChromiumPdfRenderer {

    private static final Logger log = LoggerFactory.getLogger(ChromiumPdfRenderer.class);

    private static final List<String> LAUNCH_ARGS = List.of(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-extensions");

    @Value("${pdf.renderer.pool-size:2}")
    private int poolSize;

    @Value("${pdf.renderer.acquire-timeout-ms:5000}")
    private long acquireTimeoutMs;

    @Value("${pdf.renderer.render-timeout-ms:20000}")
    private long renderTimeoutMs;

    private final BlockingQueue<Worker> idle = new LinkedBlockingQueue<>();
    private final List<Worker> all = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger workerSeq = new AtomicInteger();
    /** Chiude/rimpiazza in background i worker scartati, senza far attendere il thread chiamante. */
    private final ExecutorService cleanupExecutor = Executors.newFixedThreadPool(2,
            r -> daemonThread(r, "pdf-worker-cleanup"));

    public ChromiumPdfRenderer() {
    }

    /** Costruttore per i test diretti (no Spring context): i campi {@code @Value} restano a 0
     *  se non passati per Spring, quindi i test istanziano esplicitamente la configurazione. */
    ChromiumPdfRenderer(int poolSize, long acquireTimeoutMs, long renderTimeoutMs) {
        this.poolSize = poolSize;
        this.acquireTimeoutMs = acquireTimeoutMs;
        this.renderTimeoutMs = renderTimeoutMs;
    }

    @PostConstruct
    void start() {
        for (int i = 0; i < poolSize; i++) {
            Worker w = spawnWorker();
            all.add(w);
            idle.offer(w);
        }
        log.info("Pool di rendering PDF avviato con {} worker Chromium", poolSize);
    }

    @PreDestroy
    void stop() {
        List<Worker> snapshot;
        synchronized (all) {
            snapshot = new ArrayList<>(all);
        }
        snapshot.forEach(this::closeWorker);
        cleanupExecutor.shutdown();
    }

    /** Genera il PDF a partire dall'HTML già renderizzato da Thymeleaf. Chiamata bloccante. */
    public byte[] render(String html) {
        return render(html, renderTimeoutMs);
    }

    /** Overload con timeout esplicito, usato dai test per simulare in modo deterministico un
     *  worker che eccede il tempo massimo (senza dover costruire un HTML "lento" e flaky). */
    byte[] render(String html, long timeoutMs) {
        Worker w = acquire();
        Future<byte[]> future = w.executor.submit(() -> renderOnWorkerThread(w, html));
        try {
            byte[] pdf = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            idle.offer(w);
            return pdf;
        } catch (TimeoutException te) {
            future.cancel(true);
            // Il worker potrebbe essere bloccato in una chiamata nativa non interrompibile in
            // sicurezza: non lo si rimette in circolo, viene scartato e sostituito in background.
            discardAndRespawn(w);
            throw new PdfGenerationException("Timeout durante la generazione del PDF", te);
        } catch (ExecutionException ee) {
            idle.offer(w); // eccezione applicativa "pulita": il worker resta riusabile
            throw new PdfGenerationException("Errore del motore di rendering PDF", ee.getCause());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            discardAndRespawn(w);
            throw new PdfGenerationException("Generazione PDF interrotta", ie);
        }
    }

    private Worker acquire() {
        try {
            Worker w = idle.poll(acquireTimeoutMs, TimeUnit.MILLISECONDS);
            if (w == null) {
                throw new PdfGenerationException("Servizio di generazione PDF sovraccarico, riprova tra qualche istante");
            }
            return w;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new PdfGenerationException("Generazione PDF interrotta", ie);
        }
    }

    /** Eseguito SUL thread del worker: contesto isolato per-render, nessuna rete, nessun JS. */
    private byte[] renderOnWorkerThread(Worker w, String html) {
        try (BrowserContext ctx = w.browser.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false))) {
            // Difesa in profondità: anche se un template introducesse per errore un URL assoluto,
            // nessuna richiesta di rete deve mai lasciare il processo di rendering.
            ctx.route("**/*", route -> route.abort());
            Page page = ctx.newPage();
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            return page.pdf(new Page.PdfOptions()
                    .setPrintBackground(true)
                    .setPreferCSSPageSize(true));
        }
    }

    private Worker spawnWorker() {
        int id = workerSeq.incrementAndGet();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> daemonThread(r, "pdf-worker-" + id));
        Worker w = new Worker(id, executor);
        try {
            executor.submit(() -> {
                w.playwright = Playwright.create();
                w.browser = w.playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(LAUNCH_ARGS));
                return null;
            }).get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Avvio del worker PDF interrotto", ie);
        } catch (ExecutionException ee) {
            throw new IllegalStateException("Impossibile avviare Chromium per il rendering PDF", ee.getCause());
        }
        return w;
    }

    /** Rimpiazza in background un worker scartato, mantenendo costante la capacità nominale del pool. */
    private void discardAndRespawn(Worker old) {
        all.remove(old);
        cleanupExecutor.submit(() -> {
            closeWorker(old);
            try {
                Worker fresh = spawnWorker();
                all.add(fresh);
                idle.offer(fresh);
            } catch (Exception e) {
                log.error("Impossibile rimpiazzare il worker PDF {}: il pool ha ora un worker in meno", old.id, e);
            }
        });
    }

    private void closeWorker(Worker w) {
        try {
            w.executor.submit(() -> {
                if (w.browser != null) w.browser.close();
                if (w.playwright != null) w.playwright.close();
                return null;
            }).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Errore durante la chiusura del worker PDF {}: {}", w.id, e.getMessage());
        } finally {
            w.executor.shutdownNow();
        }
    }

    private static Thread daemonThread(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    private static final class Worker {
        final int id;
        final ExecutorService executor;
        volatile Playwright playwright;
        volatile Browser browser;

        Worker(int id, ExecutorService executor) {
            this.id = id;
            this.executor = executor;
        }
    }
}
