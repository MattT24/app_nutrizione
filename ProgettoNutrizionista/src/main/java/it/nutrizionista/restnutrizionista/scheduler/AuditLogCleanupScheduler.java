package it.nutrizionista.restnutrizionista.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;

/**
 * Retention dell'audit log (A7). Il purge è <b>disattivo di default</b> (in attesa delle durate dal
 * titolare, cfr. A6) e in ogni caso <b>non scende mai sotto i 24 mesi</b> imposti dal Garante
 * (floor di compliance, anche se la property è configurata più bassa). Modellato su
 * {@link GamificationCleanupScheduler}. Gira alle 03:30, sfalsato dalla pulizia gamification (03:00).
 */
@Component
public class AuditLogCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditLogCleanupScheduler.class);

    /** Minimo di legge: la retention dell'audit sanitario non può scendere sotto i 24 mesi. */
    static final int MIN_RETENTION_MONTHS = 24;

    private final AuditLogRepository repo;

    @Value("${app.audit.retention-purge-enabled:false}")
    private boolean purgeEnabled;

    @Value("${app.audit.retention-months:24}")
    private int retentionMonths;

    public AuditLogCleanupScheduler(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeVecchi() {
        if (!purgeEnabled) {
            return;
        }
        int mesi = Math.max(retentionMonths, MIN_RETENTION_MONTHS);
        Instant soglia = LocalDate.now().minusMonths(mesi).atStartOfDay(ZoneId.systemDefault()).toInstant();
        int eliminati = repo.deleteByCreatedAtBefore(soglia);
        if (eliminati > 0) {
            log.info("Audit: eliminate {} righe più vecchie di {} mesi", eliminati, mesi);
        }
    }
}
