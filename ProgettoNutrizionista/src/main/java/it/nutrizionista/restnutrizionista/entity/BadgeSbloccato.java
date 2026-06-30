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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Badge gamification sbloccato da un nutrizionista. Il catalogo dei badge disponibili
 * (codice, condizione di sblocco) è definito in {@code GamificationBadgeCatalogo}, non a DB.
 */
@Entity
@Table(name = "badge_sbloccati", uniqueConstraints = @UniqueConstraint(columnNames = {"utente_id", "codice_badge"}))
@EntityListeners(AuditingEntityListener.class)
public class BadgeSbloccato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente nutrizionista;

    @Column(name = "codice_badge", nullable = false, length = 64)
    private String codiceBadge;

    @CreatedDate
    @Column(name = "data_sblocco", nullable = false, updatable = false)
    private Instant dataSblocco;

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

    public String getCodiceBadge() {
        return codiceBadge;
    }

    public void setCodiceBadge(String codiceBadge) {
        this.codiceBadge = codiceBadge;
    }

    public Instant getDataSblocco() {
        return dataSblocco;
    }

    public void setDataSblocco(Instant dataSblocco) {
        this.dataSblocco = dataSblocco;
    }
}
