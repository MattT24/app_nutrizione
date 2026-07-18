package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.enums.StatoTicket;

/** Risposta dedicata del ticket, priva di riferimenti JPA e dati personali eccedenti. */
public class TicketAssistenzaDto {
    private Long id;
    private String oggetto;
    private String descrizione;
    private StatoTicket stato;
    private String motivoRifiuto;
    private Instant acceptedAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private long messaggiNonLetti;
    private NutrizionistaAssistenzaDto nutrizionista;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOggetto() { return oggetto; }
    public void setOggetto(String oggetto) { this.oggetto = oggetto; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public StatoTicket getStato() { return stato; }
    public void setStato(StatoTicket stato) { this.stato = stato; }
    public String getMotivoRifiuto() { return motivoRifiuto; }
    public void setMotivoRifiuto(String motivoRifiuto) { this.motivoRifiuto = motivoRifiuto; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getMessaggiNonLetti() { return messaggiNonLetti; }
    public void setMessaggiNonLetti(long messaggiNonLetti) { this.messaggiNonLetti = messaggiNonLetti; }
    public NutrizionistaAssistenzaDto getNutrizionista() { return nutrizionista; }
    public void setNutrizionista(NutrizionistaAssistenzaDto nutrizionista) { this.nutrizionista = nutrizionista; }
}
