package it.nutrizionista.restnutrizionista.enums;

/** Tipi di evento che generano punti gamification per il nutrizionista, con il relativo valore in punti. */
public enum TipoEventoGamification {
    NUOVO_CLIENTE(20),
    SCHEDA_CREATA(10),
    MISURAZIONE_REGISTRATA(5),
    APPUNTAMENTO_COMPLETATO(8),
    ACCESSO_GIORNALIERO(2);

    private final int punti;

    TipoEventoGamification(int punti) {
        this.punti = punti;
    }

    public int getPunti() {
        return punti;
    }
}
