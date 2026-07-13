package it.nutrizionista.restnutrizionista.enums;

/**
 * Tipo di risorsa sanitaria (dato art. 9 GDPR) oggetto di un evento di audit (A7).
 * Persistita come stringa ({@code @Enumerated(STRING)}, {@code length=48}).
 */
public enum AuditEntityType {
    CLIENTE,
    MISURAZIONE_ANTROPOMETRICA,
    PLICOMETRIA,
    SCHEDA,
    CALCOLO_TDEE,
    DOCUMENTO_FASCICOLO,
    OBIETTIVO_NUTRIZIONALE
}
