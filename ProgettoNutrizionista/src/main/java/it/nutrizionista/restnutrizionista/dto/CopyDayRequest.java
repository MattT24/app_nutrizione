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
}
