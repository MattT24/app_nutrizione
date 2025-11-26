package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

public class MacroFormDto {

 	private Long id;
    @NotBlank(message = "L'alimento base è obbligatorio")
    private AlimentoBaseDto alimento;
    @NotBlank(message = "Le calorie sono obbligatorie")
	private Double calorie;
    @NotBlank(message = "I grassi sono obbligatori")
    private Double grassi;
    @NotBlank(message = "Le proteine sono obbligatorie")
    private Double proteine;
    @NotBlank(message = "I carboidrati sono obbligatori")
    private Double carboidrati;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public AlimentoBaseDto getAlimento() {
		return alimento;
	}
	public void setAlimento(AlimentoBaseDto alimento) {
		this.alimento = alimento;
	}
	public Double getCalorie() {
		return calorie;
	}
	public void setCalorie(Double calorie) {
		this.calorie = calorie;
	}
	public Double getGrassi() {
		return grassi;
	}
	public void setGrassi(Double grassi) {
		this.grassi = grassi;
	}
	public Double getProteine() {
		return proteine;
	}
	public void setProteine(Double proteine) {
		this.proteine = proteine;
	}
	public Double getCarboidrati() {
		return carboidrati;
	}
	public void setCarboidrati(Double carboidrati) {
		this.carboidrati = carboidrati;
	}
    
    
}
