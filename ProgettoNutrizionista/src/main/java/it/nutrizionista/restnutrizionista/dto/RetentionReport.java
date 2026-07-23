package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

/**
 * Esito di un ciclo di retention (A6). Loggato dallo scheduler e asserito nei test.
 * {@code dryRun}=true → nessuna scrittura reale (solo pianificato). {@code breakerScattato}=true → il breaker
 * catastrofico ha bloccato mark+purge (il clear protettivo gira comunque).
 */
public record RetentionReport(
        boolean dryRun,
        boolean breakerScattato,
        long totaleClienti,
        int inattivi,
        int inHold,
        int pianificatiQuarantena,
        int pianificatiPurge,
        int eseguitiQuarantena,
        int eseguitiPurge,
        int purgeSaltatiToctou,
        int backlogPurge,
        int falliti,
        List<Long> idInHold,
        List<Long> idPurge) {
}
