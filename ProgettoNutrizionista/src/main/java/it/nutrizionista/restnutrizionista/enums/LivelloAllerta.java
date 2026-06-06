package it.nutrizionista.restnutrizionista.enums;

public enum LivelloAllerta {
    SAFE,           // Nessun rischio, semaforo verde/spento
    WARNING,        // Solo Warning arancione. Usato per i gusti personali e lievi intolleranze
    ALERT_GRAVE    // Semaforo rosso bloccante. Usato per Allergie o incompatibilità farmacologiche/etiche
}
