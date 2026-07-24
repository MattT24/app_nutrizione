package it.nutrizionista.restnutrizionista.service;

import com.sun.net.httpserver.HttpServer;
import it.nutrizionista.restnutrizionista.exception.PdfGenerationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica il pool di rendering Chromium senza Spring context né DB, nello stesso stile degli
 * altri test PDF del progetto: istanziazione diretta, assert su byte reali via PDFBox.
 *
 * <p>A differenza del precedente {@code PdfFontRenderingTest} (rimosso), qui non si asserisce
 * l'embedding di un font specifico: con Chromium i font vengono dai font installati sul sistema
 * operativo/immagine Docker (vedi Dockerfile), non più incorporati per-render via PDFBox — la
 * fedeltà del font brand va verificata visivamente (Fase 15 del piano), non con un unit test.
 */
class ChromiumPdfRendererTest {

    private static final String HTML_SEMPLICE =
            "<html><head><style>@page{size:A4;margin:13mm 14mm;} body{font-family:sans-serif;}</style></head>"
                    + "<body><h1>Prova àèìòù €</h1><p>Statera Nutrition</p></body></html>";

    private ChromiumPdfRenderer renderer;

    @AfterEach
    void tearDown() {
        if (renderer != null) renderer.stop();
    }

    @Test
    void renderizzaPdfValido() throws Exception {
        renderer = new ChromiumPdfRenderer(1, 5_000, 20_000);
        renderer.start();

        byte[] pdf = renderer.render(HTML_SEMPLICE);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "il PDF non deve essere vuoto");
        String header = new String(pdf, 0, Math.min(5, pdf.length), StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header, "l'output deve essere un PDF valido");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    void richiesteConcorrentiEntroLaCapacitaDelPoolCompletanoTutte() throws Exception {
        renderer = new ChromiumPdfRenderer(2, 10_000, 20_000);
        renderer.start();

        int richieste = 6;
        ExecutorService callers = Executors.newFixedThreadPool(richieste);
        try {
            List<Callable<byte[]>> tasks = new ArrayList<>();
            for (int i = 0; i < richieste; i++) tasks.add(() -> renderer.render(HTML_SEMPLICE));
            List<Future<byte[]>> futures = callers.invokeAll(tasks, 30, TimeUnit.SECONDS);
            for (Future<byte[]> f : futures) {
                byte[] pdf = f.get();
                assertNotNull(pdf);
                assertTrue(pdf.length > 0);
            }
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    void timeoutVieneGestitoEIlPoolSiAutoRipara() {
        renderer = new ChromiumPdfRenderer(1, 5_000, 20_000);
        renderer.start();

        // Timeout volutamente irraggiungibile (1ms) sulla prima chiamata: garantisce il timeout
        // in modo deterministico, senza dover costruire un HTML "lento" (flaky su macchine diverse).
        assertThrows(PdfGenerationException.class, () -> renderer.render(HTML_SEMPLICE, 1));

        // Il worker scaduto viene scartato e rimpiazzato in background (vedi discardAndRespawn):
        // una richiesta successiva con timeout ragionevole deve attendere il nuovo worker e riuscire
        // (acquire() attende fino ad acquireTimeoutMs che il worker rimpiazzato torni disponibile).
        byte[] pdf = renderer.render(HTML_SEMPLICE);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "il pool deve essersi auto-riparato dopo il timeout");
    }

    @Test
    void nessunaRichiestaDiReteVieneEffettuata() throws Exception {
        AtomicInteger richiesteRicevute = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            richiesteRicevute.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        try {
            renderer = new ChromiumPdfRenderer(1, 5_000, 20_000);
            renderer.start();

            String html = "<html><body><img src=\"http://127.0.0.1:" + server.getAddress().getPort()
                    + "/pixel.png\"><p>Nessuna rete attesa</p></body></html>";
            byte[] pdf = renderer.render(html);

            assertNotNull(pdf);
            assertEquals(0, richiesteRicevute.get(),
                    "il BrowserContext deve bloccare qualunque richiesta di rete (route abort)");
        } finally {
            server.stop(0);
        }
    }
}
