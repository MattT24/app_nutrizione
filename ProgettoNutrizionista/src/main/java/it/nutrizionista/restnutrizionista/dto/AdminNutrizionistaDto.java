package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

/**
 * Vista di un nutrizionista per la dashboard super admin.
 * Espone solo i campi necessari: niente codice fiscale, indirizzo o altri dati sensibili.
 */
public class AdminNutrizionistaDto {
    private Long id;
    private String nome;
    private String cognome;
    private String email;
    private Instant registratoIl;
    private Instant ultimoAccesso;
    private boolean attivo;
    /** "EMAIL", "GOOGLE" o null per gli utenti registrati prima del tracciamento. */
    private String metodoRegistrazione;
    /** Numero di clienti registrati dal nutrizionista. */
    private long numeroClienti;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Instant getRegistratoIl() { return registratoIl; }
    public void setRegistratoIl(Instant registratoIl) { this.registratoIl = registratoIl; }
    public Instant getUltimoAccesso() { return ultimoAccesso; }
    public void setUltimoAccesso(Instant ultimoAccesso) { this.ultimoAccesso = ultimoAccesso; }
    public boolean isAttivo() { return attivo; }
    public void setAttivo(boolean attivo) { this.attivo = attivo; }
    public String getMetodoRegistrazione() { return metodoRegistrazione; }
    public void setMetodoRegistrazione(String metodoRegistrazione) { this.metodoRegistrazione = metodoRegistrazione; }
    public long getNumeroClienti() { return numeroClienti; }
    public void setNumeroClienti(long numeroClienti) { this.numeroClienti = numeroClienti; }
}
