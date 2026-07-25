package it.nutrizionista.restnutrizionista.entity;

import it.nutrizionista.restnutrizionista.enums.MetodoRegistrazione;
import it.nutrizionista.restnutrizionista.enums.TemaPdf;
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

    /**
     * Keycloak subject ({@code sub}) — link identità IdP ↔ app (Fase 2 Keycloak). NULL finché non avviene il
     * primo login OIDC (link gated su {@code email_verified}). Chiave DOPPIA: risoluzione del principal
     * ({@code CurrentUserService.getMe}) + erasure key (delete cross-store art. 17). Unique nullable → più NULL ammessi.
     */
    @Column(name = "subject_id", unique = true)
    private String subjectId;

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

    /** Ultimo login riuscito (credenziali o Google): base per lo stato attivo/inattivo in dashboard admin. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Come è avvenuta la registrazione (EMAIL o GOOGLE); null per gli utenti precedenti a questo campo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_registrazione")
    private MetodoRegistrazione metodoRegistrazione;

    @Column(name = "logo")
    private String filePathLogo;

    /** Tema colore preferito per i PDF generati (preferenza globale del nutrizionista). */
    @Enumerated(EnumType.STRING)
    @Column(name = "pdf_tema_colore", length = 16)
    private TemaPdf pdfTemaColore = TemaPdf.EMERALD;

    // A6 / F-USER-DEL: cascade RIMOSSO di proposito. La cancellazione dei clienti passa SOLO dal loop
    // esplicito in UtenteService.deleteAccount → ClienteService.eliminaClienteCompleto (audit + PDF su disco
    // + anonimizzazione gamification). Un cascade ORM qui li cancellerebbe "di nascosto" bypassando quel metodo.
    @OneToMany(mappedBy = "nutrizionista", fetch = FetchType.LAZY)
    private List<Cliente> clienti;

    @OneToMany(mappedBy = "utente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UtentePreferito> preferiti;

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
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
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
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public MetodoRegistrazione getMetodoRegistrazione() { return metodoRegistrazione; }
    public void setMetodoRegistrazione(MetodoRegistrazione metodoRegistrazione) { this.metodoRegistrazione = metodoRegistrazione; }
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
	public TemaPdf getPdfTemaColore() {
		return pdfTemaColore;
	}
	public void setPdfTemaColore(TemaPdf pdfTemaColore) {
		this.pdfTemaColore = pdfTemaColore;
	}
    

	
}