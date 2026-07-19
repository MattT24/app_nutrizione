package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

/** Messaggio sicuro per la UI; mioMessaggio è calcolato rispetto al JWT chiamante. */
public class MessaggioAssistenzaDto {
    private Long id;
    private String testo;
    private boolean letto;
    private boolean mioMessaggio;
    private Instant createdAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }
    public boolean isLetto() { return letto; }
    public void setLetto(boolean letto) { this.letto = letto; }
    public boolean isMioMessaggio() { return mioMessaggio; }
    public void setMioMessaggio(boolean mioMessaggio) { this.mioMessaggio = mioMessaggio; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
