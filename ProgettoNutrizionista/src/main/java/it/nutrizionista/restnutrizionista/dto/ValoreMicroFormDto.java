package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

public class ValoreMicroFormDto {
	
    private Long id;

    @NotNull(message = "Il micronutriente è obbligatorio")
    private MicroDto micronutriente;

    private Double valore;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MicroDto getMicronutriente() {
		return micronutriente;
	}

	public void setMicronutriente(MicroDto micronutriente) {
		this.micronutriente = micronutriente;
	}

	public Double getValore() {
		return valore;
	}

	public void setValore(Double valore) {
		this.valore = valore;
	}
    
}
