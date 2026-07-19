package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import it.nutrizionista.restnutrizionista.enums.StatoTicket;

/** Payload incrementale della chat, comprensivo dello stato corrente del ticket. */
public class PollingMessaggiAssistenzaDto {
    private StatoTicket stato;
    private List<MessaggioAssistenzaDto> messaggi;
    public StatoTicket getStato() { return stato; }
    public void setStato(StatoTicket stato) { this.stato = stato; }
    public List<MessaggioAssistenzaDto> getMessaggi() { return messaggi; }
    public void setMessaggi(List<MessaggioAssistenzaDto> messaggi) { this.messaggi = messaggi; }
}
