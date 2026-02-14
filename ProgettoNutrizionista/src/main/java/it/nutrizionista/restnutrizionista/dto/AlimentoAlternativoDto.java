package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.entity.AlternativeMode;

/**
 * DTO per la lettura di un AlimentoAlternativo
 */
public class AlimentoAlternativoDto {

    private Long id;
    private AlimentoPastoDto alimentoPasto;
    private AlimentoBaseDto alimentoAlternativo;
    private Integer quantita;
    private Integer priorita;
    private AlternativeMode mode;
    private Boolean manual;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AlimentoPastoDto getAlimentoPasto() {
        return alimentoPasto;
    }

    public void setAlimentoPasto(AlimentoPastoDto alimentoPasto) {
        this.alimentoPasto = alimentoPasto;
    }

    public AlimentoBaseDto getAlimentoAlternativo() {
        return alimentoAlternativo;
    }

    public void setAlimentoAlternativo(AlimentoBaseDto alimentoAlternativo) {
        this.alimentoAlternativo = alimentoAlternativo;
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
