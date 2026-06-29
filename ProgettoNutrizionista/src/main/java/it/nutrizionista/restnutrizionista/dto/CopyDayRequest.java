package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import it.nutrizionista.restnutrizionista.entity.GiornoSettimana;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class CopyDayRequest {
    
    @NotNull(message = "Il giorno sorgente è obbligatorio")
    private GiornoSettimana sourceDay;
    
    @NotEmpty(message = "Specificare almeno un giorno di destinazione")
    private List<GiornoSettimana> targetDays;

    private List<Long> alimentoPastoIds;

    /** Opzionale: id dei pasti sorgente da copiare (se vuoto, tutti i pasti del giorno). */
    private List<Long> selectedPastoIds;

    /** "REPLACE" (default): sostituisce i pasti dei giorni target; "ADD": li aggiunge a quelli presenti. */
    private String mode;

    public GiornoSettimana getSourceDay() {
        return sourceDay;
    }

    public void setSourceDay(GiornoSettimana sourceDay) {
        this.sourceDay = sourceDay;
    }

    public List<GiornoSettimana> getTargetDays() {
        return targetDays;
    }

    public void setTargetDays(List<GiornoSettimana> targetDays) {
        this.targetDays = targetDays;
    }

    public List<Long> getAlimentoPastoIds() {
        return alimentoPastoIds;
    }

    public void setAlimentoPastoIds(List<Long> alimentoPastoIds) {
        this.alimentoPastoIds = alimentoPastoIds;
    }

    public List<Long> getSelectedPastoIds() {
        return selectedPastoIds;
    }

    public void setSelectedPastoIds(List<Long> selectedPastoIds) {
        this.selectedPastoIds = selectedPastoIds;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
