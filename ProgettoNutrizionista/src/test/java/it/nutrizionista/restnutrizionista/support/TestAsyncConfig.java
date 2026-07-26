package it.nutrizionista.restnutrizionista.support;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * In profilo test gli executor async sono resi <b>deterministici</b> (l'evento è concluso quando il
 * metodo {@code @Async} ritorna → gli assert non devono attendere), ma con due strategie diverse:
 * <ul>
 *   <li>{@code taskExecutor} (default: email {@code @Async}, correlation-id) → {@link SyncTaskExecutor}
 *       inline, sullo stesso thread del chiamante. Le email non toccano il DB, quindi va bene.</li>
 *   <li>{@code auditExecutor} (audit letture/liste A7 → {@code AuditService.persistAsync}) → eseguito su
 *       un <b>thread separato</b> e atteso ({@code join}), come in produzione (executor reale): l'INSERT su
 *       {@code audit_log} usa una <b>connessione propria non-read-only</b>, NON quella della transazione
 *       {@code readOnly=true} del metodo di lettura chiamante. Un {@link SyncTaskExecutor} collasserebbe
 *       l'INSERT su quella connessione: innocuo su H2 (non applica {@code setReadOnly}), ma un MySQL/TiDB
 *       reale rifiuta la scrittura ("Connection is read-only") → il job CI {@code test-mysql} falliva.
 *       È sicuro su un thread separato perché {@code audit_log} è senza FK e {@code save} usa solo il
 *       {@code AuditContext} già catturato dal chiamante (nessun thread-local); i test non sono
 *       {@code @Transactional}, quindi la riga committata è visibile all'assert successivo.</li>
 * </ul>
 * Sostituiscono i {@code ThreadPoolTaskExecutor} di produzione (annotati {@code @Profile("!test")}).
 */
@Configuration
@Profile("test")
public class TestAsyncConfig {

    /**
     * Audit su thread separato + {@code join}: connessione propria (non-read-only, come l'async di prod)
     * ma deterministico. Vedi Javadoc di classe per il razionale (parità H2↔MySQL nel job CI test-mysql).
     */
    @Bean("auditExecutor")
    public Executor auditExecutor() {
        return task -> {
            Thread t = new Thread(task, "test-audit-executor");
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrotto in attesa dell'audit (test)", e);
            }
        };
    }

    /** Follow-up A1: default {@code @Async} sincrono in test (le email {@code @Async} non diventano flaky). */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
