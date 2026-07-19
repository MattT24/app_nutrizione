package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Singolo messaggio testuale della chat associata a un ticket di assistenza. */
@Entity
@Table(name = "messaggi_assistenza", indexes = {
        @Index(name = "idx_messaggi_assistenza_ticket_id", columnList = "ticket_id, id")
})
@EntityListeners(AuditingEntityListener.class)
public class MessaggioAssistenza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketAssistenza ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mittente_id", nullable = false)
    private Utente mittente;

    @Column(nullable = false, length = 2000)
    private String testo;

    @Column(nullable = false)
    private boolean letto = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TicketAssistenza getTicket() { return ticket; }
    public void setTicket(TicketAssistenza ticket) { this.ticket = ticket; }
    public Utente getMittente() { return mittente; }
    public void setMittente(Utente mittente) { this.mittente = mittente; }
    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }
    public boolean isLetto() { return letto; }
    public void setLetto(boolean letto) { this.letto = letto; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
