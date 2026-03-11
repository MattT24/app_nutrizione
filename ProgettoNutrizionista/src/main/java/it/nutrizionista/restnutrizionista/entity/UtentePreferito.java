package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Entità pivot esplicita per la gestione dei preferiti (relazione M:N tra Utente e AlimentoBase).
 */
@Entity
@Table(
    name = "utente_preferiti",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_utente_alimento", columnNames = {"utente_id", "alimento_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class UtentePreferito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id", nullable = false)
    private AlimentoBase alimento;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UtentePreferito() {}

    public UtentePreferito(Utente utente, AlimentoBase alimento) {
        this.utente = utente;
        this.alimento = alimento;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utente getUtente() { return utente; }
    public void setUtente(Utente utente) { this.utente = utente; }

    public AlimentoBase getAlimento() { return alimento; }
    public void setAlimento(AlimentoBase a) { this.alimento = a; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
