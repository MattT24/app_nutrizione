package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;

/**
 * Log append-only degli eventi che generano punti gamification per il nutrizionista.
 * Una riga per ogni azione rilevante (nuovo cliente, scheda creata, ecc.); per
 * ACCESSO_GIORNALIERO se ne registra al più una per giorno (controllo applicativo nel service).
 */
@Entity
@Table(name = "eventi_gamification", indexes = {
        @Index(name = "idx_eventi_gamification_utente_tipo_data", columnList = "utente_id, tipo_evento, created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class EventoGamification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente nutrizionista;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false)
    private TipoEventoGamification tipoEvento;

    @Column(nullable = false)
    private int punti;

    @Column(name = "cliente_id")
    private Long clienteId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utente getNutrizionista() {
        return nutrizionista;
    }

    public void setNutrizionista(Utente nutrizionista) {
        this.nutrizionista = nutrizionista;
    }

    public TipoEventoGamification getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEventoGamification tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public int getPunti() {
        return punti;
    }

    public void setPunti(int punti) {
        this.punti = punti;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
