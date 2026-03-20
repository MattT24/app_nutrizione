package it.nutrizionista.restnutrizionista.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class OrariStudioFormDto {

    private DayOfWeek giornoSettimana;
    private boolean giornoLavorativo;
    
    private LocalTime oraApertura;
    private LocalTime oraChiusura;
    
    private LocalTime inizioPausaPranzo;
    private LocalTime finePausaPranzo;

    public OrariStudioFormDto() {
    }

    public DayOfWeek getGiornoSettimana() {
        return giornoSettimana;
    }

    public void setGiornoSettimana(DayOfWeek giornoSettimana) {
        this.giornoSettimana = giornoSettimana;
    }

    public boolean isGiornoLavorativo() {
        return giornoLavorativo;
    }

    public void setGiornoLavorativo(boolean giornoLavorativo) {
        this.giornoLavorativo = giornoLavorativo;
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

    public LocalTime getInizioPausaPranzo() {
        return inizioPausaPranzo;
    }

    public void setInizioPausaPranzo(LocalTime inizioPausaPranzo) {
        this.inizioPausaPranzo = inizioPausaPranzo;
    }

    public LocalTime getFinePausaPranzo() {
        return finePausaPranzo;
    }

    public void setFinePausaPranzo(LocalTime finePausaPranzo) {
        this.finePausaPranzo = finePausaPranzo;
    }
}