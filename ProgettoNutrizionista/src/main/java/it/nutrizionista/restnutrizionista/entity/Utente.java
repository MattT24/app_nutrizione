package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Entità Utente: appartiene ad un Ruolo.
 */
@Entity
@Table(name = "utenti")
@EntityListeners(AuditingEntityListener.class)
public class Utente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String nome;
    @Column(nullable = false) private String cognome;

    @Column(name = "codice_fiscale", nullable = false, unique = true)
    private String codiceFiscale;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false) private String password;

    @Column(name = "data_nascita")
    private LocalDate dataNascita;
    
    @Column(nullable = false)
    private String telefono;
    
    @Column(nullable = false)
    private String indirizzo;

    /** Ruolo assegnato all'utente. */
    @ManyToOne
    @JoinColumn(name = "ruolo_id")
    private Ruolo ruolo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "logo")
    private String filePathLogo;
    
    @OneToMany(mappedBy = "nutrizionista", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Cliente> clienti;

    // Getter/Setter
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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDate getDataNascita() { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    public Ruolo getRuolo() { return ruolo; }
    public void setRuolo(Ruolo ruolo) { this.ruolo = ruolo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
	public String getFilePathLogo() {
		return filePathLogo;
	}
	public void setFilePathLogo(String filePathLogo) {
		this.filePathLogo = filePathLogo;
	}
	public List<Cliente> getClienti() {
		return clienti;
	}
	public void setClienti(List<Cliente> clienti) {
		this.clienti = clienti;
	}
    

	
}