package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** DTO di lettura per Utente (include il Ruolo come DTO). */
public class UtenteDto {
    private Long id;
    private String nome;
    private String cognome;
    private String codiceFiscale;
    private String email;
    private LocalDate dataNascita;
    private String telefono;
    private String indirizzo;
    private RuoloDto ruolo;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    public String getCodiceFiscale() { return codiceFiscale; }
    public void setCodiceFiscale(String codiceFiscale) { this.codiceFiscale = codiceFiscale; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getDataNascita() { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    public RuoloDto getRuolo() { return ruolo; }
    public void setRuolo(RuoloDto ruolo) { this.ruolo = ruolo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    
}
