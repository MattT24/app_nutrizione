package it.nutrizionista.restnutrizionista.enums;

/**
 * Documenti legali che il nutrizionista accetta/prende visione alla registrazione (A4/A9).
 * NB: base giuridica = contratto (art. 6(1)(b)), non "consenso": è accettazione di documenti versionati.
 * Persistito come stringa ({@code @Enumerated(STRING)}, {@code length=32}).
 */
public enum TipoDocumento {
    /** Informativa privacy — presa visione (art. 13, trasparenza). */
    PRIVACY_POLICY,
    /** Termini di Servizio — accettazione contrattuale (art. 6(1)(b)). */
    TERMINI_SERVIZIO,
    /** Data Processing Agreement — accordo titolare↔responsabile (art. 28). */
    DPA
}
