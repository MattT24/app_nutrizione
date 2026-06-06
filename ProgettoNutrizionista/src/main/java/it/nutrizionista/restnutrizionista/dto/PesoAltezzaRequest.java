package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

public class PesoAltezzaRequest {
    @NotNull(message = "Id cliente obbligatorio")
    private Long id;
    
    private Double peso;
    private Integer altezza;
    private Double pesoTarget;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public Integer getAltezza() {
        return altezza;
    }

    public void setAltezza(Integer altezza) {
        this.altezza = altezza;
    }

    public Double getPesoTarget() {
        return pesoTarget;
    }

    public void setPesoTarget(Double pesoTarget) {
        this.pesoTarget = pesoTarget;
    }
}
