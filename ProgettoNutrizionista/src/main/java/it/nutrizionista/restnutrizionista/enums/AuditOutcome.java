package it.nutrizionista.restnutrizionista.enums;

/**
 * Esito di un evento di audit (A7). Persistito come stringa ({@code @Enumerated(STRING)}, {@code length=16}).
 */
public enum AuditOutcome {
    /** Operazione consentita ed eseguita. */
    SUCCESS,
    /** Accesso negato (ownership/autorizzazione fallita). */
    DENIED,
    /** Operazione autorizzata ma fallita nell'esecuzione (es. invio email non riuscito). */
    FAILURE
}
