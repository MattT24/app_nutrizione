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
    private Double fibre;
    private Double zuccheri;
    private Double grassiSaturi;
    private Double sodio;
    private Double alcol;
    private Double acqua;
    
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
	public Double getFibre() {
		return fibre;
	}
	public void setFibre(Double fibre) {
		this.fibre = fibre;
	}
	public Double getZuccheri() {
		return zuccheri;
	}
	public void setZuccheri(Double zuccheri) {
		this.zuccheri = zuccheri;
	}
	public Double getGrassiSaturi() {
		return grassiSaturi;
	}
	public void setGrassiSaturi(Double grassiSaturi) {
		this.grassiSaturi = grassiSaturi;
	}
	public Double getSodio() {
		return sodio;
	}
	public void setSodio(Double sodio) {
		this.sodio = sodio;
	}
	public Double getAlcol() {
		return alcol;
	}
	public void setAlcol(Double alcol) {
		this.alcol = alcol;
	}
	public Double getAcqua() {
		return acqua;
	}
	public void setAcqua(Double acqua) {
		this.acqua = acqua;
	}
    
}

