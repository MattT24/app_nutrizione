package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.enums.TipoDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Accettazione di un documento legale (Privacy/Termini/DPA) da parte di un Utente (nutrizionista),
 * con la versione del documento e la data. La base giuridica dell'account B2B è il **contratto**
 * (art. 6(1)(b)): questa entità modella la **presa visione / accettazione di un documento versionato**,
 * NON un "consenso al trattamento". Append-only: una riga per accettazione; l'unica mutazione è la
 * {@link #revoca(Instant)} (valorizza {@code revocatoAt}). Il timestamp è impostato a mano dal service
 * (nessun {@code AuditingEntityListener}) per non lasciare ambiguità su chi popola {@code accettatoAt}.
 */
@Entity
@Table(name = "accettazioni_documento", indexes = {
        @Index(name = "idx_accettazione_utente", columnList = "utente_id, tipo")
})
public class AccettazioneDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 32)
    private TipoDocumento tipo;

    /** Versione del documento accettato (es. "v1"). Al cambio versione serve ri-accettazione (vedi AccettazioneService). */
    @Column(name = "versione", nullable = false, length = 32)
    private String versione;

    @Column(name = "accettato_at", nullable = false, updatable = false)
    private Instant accettatoAt;

    /** Valorizzato alla revoca. Per Termini/DPA la revoca = cessazione contrattuale (conseguenza a valle differita). */
    @Column(name = "revocato_at")
    private Instant revocatoAt;

    protected AccettazioneDocumento() {
    }

    public AccettazioneDocumento(Utente utente, TipoDocumento tipo, String versione, Instant accettatoAt) {
        this.utente = utente;
        this.tipo = tipo;
        this.versione = versione;
        this.accettatoAt = accettatoAt;
    }

    /** Registra la revoca dell'accettazione. */
    public void revoca(Instant quando) {
        this.revocatoAt = quando;
    }

    public Long getId() {
        return id;
    }

    public Utente getUtente() {
        return utente;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String getVersione() {
        return versione;
    }

    public Instant getAccettatoAt() {
        return accettatoAt;
    }

    public Instant getRevocatoAt() {
        return revocatoAt;
    }
}
