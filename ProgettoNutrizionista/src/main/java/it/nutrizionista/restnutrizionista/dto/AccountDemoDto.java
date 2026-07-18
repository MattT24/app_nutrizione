package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.enums.StatoAccountDemo;

/** Vista amministrativa senza hash, email tecnica o altri campi interni. */
public class AccountDemoDto {
    private Long id;
    private Long utenteId;
    private String username;
    private StatoAccountDemo stato;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant lastLoginAt;
    private Long createdByAdminId;
    private long numeroClientiRegistrati;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUtenteId() { return utenteId; }
    public void setUtenteId(Long utenteId) { this.utenteId = utenteId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public StatoAccountDemo getStato() { return stato; }
    public void setStato(StatoAccountDemo stato) { this.stato = stato; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public long getNumeroClientiRegistrati() { return numeroClientiRegistrati; }
    public void setNumeroClientiRegistrati(long numeroClientiRegistrati) {
        this.numeroClientiRegistrati = numeroClientiRegistrati;
    }
}
