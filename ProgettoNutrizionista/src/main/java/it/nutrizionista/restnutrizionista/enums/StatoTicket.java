package it.nutrizionista.restnutrizionista.enums;

/**
 * Stati del ticket di assistenza. Transizioni consentite (validate SOLO lato server):
 * IN_ATTESA -> ACCETTATO | RIFIUTATO (solo super admin)
 * IN_ATTESA -> CHIUSO (il nutrizionista annulla la propria richiesta)
 * ACCETTATO -> CHIUSO (super admin o nutrizionista proprietario)
 */
public enum StatoTicket {
    IN_ATTESA,
    ACCETTATO,
    RIFIUTATO,
    CHIUSO
}
