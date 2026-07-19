package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.nutrizionista.restnutrizionista.enums.StatoTicket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import jakarta.persistence.Version;

/** Richiesta di assistenza aperta da un nutrizionista verso il super admin. */
@Entity
@Table(name = "ticket_assistenza", indexes = {
        @Index(name = "idx_ticket_assistenza_stato", columnList = "stato"),
        @Index(name = "idx_ticket_assistenza_nutrizionista_stato", columnList = "nutrizionista_id, stato")
})
@EntityListeners(AuditingEntityListener.class)
public class TicketAssistenza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutrizionista_id", nullable = false)
    private Utente nutrizionista;

    @Column(nullable = false, length = 120)
    private String oggetto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descrizione;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatoTicket stato;

    @Column(name = "motivo_rifiuto", length = 500)
    private String motivoRifiuto;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Impedisce che due admin accettino o rifiutino contemporaneamente lo stesso ticket. */
    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Utente getNutrizionista() { return nutrizionista; }
    public void setNutrizionista(Utente nutrizionista) { this.nutrizionista = nutrizionista; }
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
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
