package it.nutrizionista.restnutrizionista.enums;

/**
 * Tipo di risorsa oggetto di un evento di audit (A7). La maggior parte sono dati sanitari
 * (art. 9 GDPR); i valori aggiunti per F-OWN-SWEEP servono a qualificare l'{@code entityId} negli
 * eventi di accesso NEGATO (ACCESS/DENIED) sulle sub-risorse del piano e sulle risorse operative del
 * nutrizionista — quindi {@code PASTO}/{@code ALIMENTO_PASTO} sono art. 9, mentre {@code APPUNTAMENTO}
 * è un tipo ai soli fini dell'audit d'accesso, NON dati art. 9.
 * Persistita come stringa ({@code @Enumerated(STRING)}, {@code length=48}).
 */
public enum AuditEntityType {
    CLIENTE,
    MISURAZIONE_ANTROPOMETRICA,
    PLICOMETRIA,
    SCHEDA,
    CALCOLO_TDEE,
    DOCUMENTO_FASCICOLO,
    OBIETTIVO_NUTRIZIONALE,
    // F-OWN-SWEEP: qualificano l'entityId nei DENIED sulle sub-risorse/operative (vedi Javadoc).
    PASTO,
    ALIMENTO_PASTO,
    APPUNTAMENTO
}
