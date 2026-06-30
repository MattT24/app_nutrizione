package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Punti totali accumulati dal nutrizionista (gamification). Una sola riga per nutrizionista,
 * aggiornata in upsert ad ogni evento (stesso pattern di {@link AttivitaRecente}).
 */
@Entity
@Table(name = "progressione_nutrizionista", uniqueConstraints = @UniqueConstraint(columnNames = "utente_id"))
@EntityListeners(AuditingEntityListener.class)
public class ProgressioneNutrizionista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente nutrizionista;

    @Column(name = "punti_totali", nullable = false)
    private int puntiTotali;

    /**
     * Punti "spendibili" per i premi (es. mese gratis di abbonamento): a differenza di
     * {@link #puntiTotali} (a vita, usato per i livelli) questo saldo viene scalato quando un
     * premio viene riscattato, quindi può scendere e risalire nel tempo.
     */
    @Column(name = "punti_riscattabili", nullable = false)
    @ColumnDefault("0")
    private int puntiRiscattabili;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

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

    public int getPuntiTotali() {
        return puntiTotali;
    }

    public void setPuntiTotali(int puntiTotali) {
        this.puntiTotali = puntiTotali;
    }

    public int getPuntiRiscattabili() {
        return puntiRiscattabili;
    }

    public void setPuntiRiscattabili(int puntiRiscattabili) {
        this.puntiRiscattabili = puntiRiscattabili;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
