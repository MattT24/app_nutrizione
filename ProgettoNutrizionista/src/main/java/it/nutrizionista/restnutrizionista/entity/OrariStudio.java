package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orari_studio")
@EntityListeners(AuditingEntityListener.class)
public class OrariStudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un set di orari per ogni nutrizionista
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false, unique = true)
    private Utente nutrizionista;

    @Column(nullable = false)
    private LocalTime oraApertura;

    @Column(nullable = false)
    private LocalTime oraChiusura;

    // Pausa pranzo (opzionale)
    private LocalTime pausaInizio;
    private LocalTime pausaFine;

    @Column(nullable = false)
    private boolean lavoraSabato;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
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

    public LocalTime getOraApertura() {
        return oraApertura;
    }

    public void setOraApertura(LocalTime oraApertura) {
        this.oraApertura = oraApertura;
    }

    public LocalTime getOraChiusura() {
        return oraChiusura;
    }

    public void setOraChiusura(LocalTime oraChiusura) {
        this.oraChiusura = oraChiusura;
    }

    public LocalTime getPausaInizio() {
        return pausaInizio;
    }

    public void setPausaInizio(LocalTime pausaInizio) {
        this.pausaInizio = pausaInizio;
    }

    public LocalTime getPausaFine() {
        return pausaFine;
    }

    public void setPausaFine(LocalTime pausaFine) {
        this.pausaFine = pausaFine;
    }

    public boolean isLavoraSabato() {
        return lavoraSabato;
    }

    public void setLavoraSabato(boolean lavoraSabato) {
        this.lavoraSabato = lavoraSabato;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
