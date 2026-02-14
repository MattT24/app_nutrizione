package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;

/**
 * DTO per create/update di un AlimentoAlternativo
 */
public class AlimentoAlternativoFormDto {

    private Long id;

    @NotNull(message = "L'ID dell'alimento in pasto è obbligatorio")
    private Long alimentoPastoId;

    @NotNull(message = "L'ID dell'alimento alternativo è obbligatorio")
    private Long alimentoAlternativoId;

    @NotNull(message = "La quantità è obbligatoria")
    @Min(value = 1, message = "La quantità deve essere almeno 1 grammo")
    private Integer quantita = 100;

    @Min(value = 1, message = "La priorità deve essere almeno 1")
    private Integer priorita = 1;

    private AlternativeMode mode = AlternativeMode.CALORIE;

    private Boolean manual = false;

    private String note;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlimentoPastoId() {
        return alimentoPastoId;
    }

    public void setAlimentoPastoId(Long alimentoPastoId) {
        this.alimentoPastoId = alimentoPastoId;
    }

    public Long getAlimentoAlternativoId() {
        return alimentoAlternativoId;
    }

    public void setAlimentoAlternativoId(Long alimentoAlternativoId) {
        this.alimentoAlternativoId = alimentoAlternativoId;
    }

    public Integer getQuantita() {
        return quantita;
    }

    public void setQuantita(Integer quantita) {
        this.quantita = quantita;
    }

    public Integer getPriorita() {
        return priorita;
    }

    public void setPriorita(Integer priorita) {
        this.priorita = priorita;
    }

    public AlternativeMode getMode() {
        return mode;
    }

    public void setMode(AlternativeMode mode) {
        this.mode = mode;
    }

    public Boolean getManual() {
        return manual;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
