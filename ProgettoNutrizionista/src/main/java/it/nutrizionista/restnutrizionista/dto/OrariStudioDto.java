package it.nutrizionista.restnutrizionista.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public class OrariStudioDto {

    private Long id;
    private Long nutrizionistaId;
    private DayOfWeek giornoSettimana;
    private boolean giornoLavorativo;
    private LocalTime oraApertura;
    private LocalTime oraChiusura;
    private LocalTime inizioPausaPranzo;
    private LocalTime finePausaPranzo;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNutrizionistaId() { return nutrizionistaId; }
    public void setNutrizionistaId(Long nutrizionistaId) { this.nutrizionistaId = nutrizionistaId; }

    public DayOfWeek getGiornoSettimana() { return giornoSettimana; }
    public void setGiornoSettimana(DayOfWeek giornoSettimana) { this.giornoSettimana = giornoSettimana; }

    public boolean isGiornoLavorativo() { return giornoLavorativo; }
    public void setGiornoLavorativo(boolean giornoLavorativo) { this.giornoLavorativo = giornoLavorativo; }

    public LocalTime getOraApertura() { return oraApertura; }
    public void setOraApertura(LocalTime oraApertura) { this.oraApertura = oraApertura; }

    public LocalTime getOraChiusura() { return oraChiusura; }
    public void setOraChiusura(LocalTime oraChiusura) { this.oraChiusura = oraChiusura; }

    public LocalTime getInizioPausaPranzo() { return inizioPausaPranzo; }
    public void setInizioPausaPranzo(LocalTime inizioPausaPranzo) { this.inizioPausaPranzo = inizioPausaPranzo; }

    public LocalTime getFinePausaPranzo() { return finePausaPranzo; }
    public void setFinePausaPranzo(LocalTime finePausaPranzo) { this.finePausaPranzo = finePausaPranzo; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}