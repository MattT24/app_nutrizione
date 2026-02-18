package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalTime;

public class OrariStudioDto {

    private Long id;
    private Long nutrizionistaId;

    private LocalTime oraApertura;
    private LocalTime oraChiusura;

    private LocalTime pausaInizio;
    private LocalTime pausaFine;

    private boolean lavoraSabato;

    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNutrizionistaId() {
        return nutrizionistaId;
    }

    public void setNutrizionistaId(Long nutrizionistaId) {
        this.nutrizionistaId = nutrizionistaId;
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
