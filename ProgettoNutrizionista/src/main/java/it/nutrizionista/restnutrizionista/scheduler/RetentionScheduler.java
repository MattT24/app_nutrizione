package it.nutrizionista.restnutrizionista.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import it.nutrizionista.restnutrizionista.dto.RetentionReport;
import it.nutrizionista.restnutrizionista.service.RetentionService;

/**
 * A6 — trigger notturno del ciclo di retention (art. 5(1)(e)/art. 17). Gira alle 04:00, sfalsato dagli altri
 * scheduler (gamification 03:00, audit 03:30). Non {@code @Transactional}/{@code @Async}: orchestra soltanto;
 * la logica (con dry-run/quarantena/breaker/rate-limit/TOCTOU) è in {@link RetentionService}.
 */
@Component
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final RetentionService retentionService;

    public RetentionScheduler(RetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void esegui() {
        RetentionReport report = retentionService.eseguiCicloRetention();
        if (report != null) {
            log.info("Retention: ciclo completato — {}", report);
        }
    }
}
