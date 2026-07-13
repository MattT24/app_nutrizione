package it.nutrizionista.restnutrizionista.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;

/**
 * Retention audit (A7): purge disattivo di default → nessuna cancellazione; con purge attivo la
 * soglia è comunque clampata a ≥ 24 mesi (floor di compliance) anche se la property è più bassa.
 */
class AuditLogCleanupSchedulerTest {

    @Test
    void purgeDisattivo_nonCancellaNulla() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        AuditLogCleanupScheduler scheduler = new AuditLogCleanupScheduler(repo);
        ReflectionTestUtils.setField(scheduler, "purgeEnabled", false);
        ReflectionTestUtils.setField(scheduler, "retentionMonths", 24);

        scheduler.purgeVecchi();

        verify(repo, never()).deleteByCreatedAtBefore(any());
    }

    @Test
    void purgeAttivo_clampAMinimo24Mesi() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        AuditLogCleanupScheduler scheduler = new AuditLogCleanupScheduler(repo);
        ReflectionTestUtils.setField(scheduler, "purgeEnabled", true);
        ReflectionTestUtils.setField(scheduler, "retentionMonths", 6); // sotto il floor: deve essere ignorato

        scheduler.purgeVecchi();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repo).deleteByCreatedAtBefore(captor.capture());
        Instant soglia = captor.getValue();

        Instant atteso24Mesi = LocalDate.now().minusMonths(24).atStartOfDay(ZoneId.systemDefault()).toInstant();
        assertTrue(Math.abs(Duration.between(soglia, atteso24Mesi).toHours()) < 24,
                "la soglia deve corrispondere a ~24 mesi fa (clamp), non a 6");
        Instant seiMesiFa = LocalDate.now().minusMonths(6).atStartOfDay(ZoneId.systemDefault()).toInstant();
        assertTrue(soglia.isBefore(seiMesiFa), "la soglia deve essere più vecchia di 6 mesi fa");
    }
}
