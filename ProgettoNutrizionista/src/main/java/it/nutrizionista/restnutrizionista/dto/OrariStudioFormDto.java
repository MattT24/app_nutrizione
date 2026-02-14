package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalTime;

public class OrariStudioFormDto {

    private LocalTime oraApertura;
    private LocalTime oraChiusura;

    private LocalTime pausaInizio;
    private LocalTime pausaFine;

    private boolean lavoraSabato;

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
}
