package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.TipoObiettivo;
import jakarta.validation.constraints.NotNull;

public class ObiettivoNutrizionaleFormDto {

	@NotNull(message = "L'obiettivo è obbligatorio")
	private TipoObiettivo obiettivo;

	private Double targetCalorie;
	private Double targetProteine;
	private Double targetCarboidrati;
	private Double targetGrassi;
	private Double targetFibre;

	private Double pctProteine;
	private Double pctCarboidrati;
	private Double pctGrassi;

	private String note;

	// Getters & Setters
	public TipoObiettivo getObiettivo() {
		return obiettivo;
	}

	public void setObiettivo(TipoObiettivo obiettivo) {
		this.obiettivo = obiettivo;
	}

	public Double getTargetCalorie() {
		return targetCalorie;
	}

	public void setTargetCalorie(Double targetCalorie) {
		this.targetCalorie = targetCalorie;
	}

	public Double getTargetProteine() {
		return targetProteine;
	}

	public void setTargetProteine(Double targetProteine) {
		this.targetProteine = targetProteine;
	}

	public Double getTargetCarboidrati() {
		return targetCarboidrati;
	}

	public void setTargetCarboidrati(Double targetCarboidrati) {
		this.targetCarboidrati = targetCarboidrati;
	}

	public Double getTargetGrassi() {
		return targetGrassi;
	}

	public void setTargetGrassi(Double targetGrassi) {
		this.targetGrassi = targetGrassi;
	}

	public Double getTargetFibre() {
		return targetFibre;
	}

	public void setTargetFibre(Double targetFibre) {
		this.targetFibre = targetFibre;
	}

	public Double getPctProteine() {
		return pctProteine;
	}

	public void setPctProteine(Double pctProteine) {
		this.pctProteine = pctProteine;
	}

	public Double getPctCarboidrati() {
		return pctCarboidrati;
	}

	public void setPctCarboidrati(Double pctCarboidrati) {
		this.pctCarboidrati = pctCarboidrati;
	}

	public Double getPctGrassi() {
		return pctGrassi;
	}

	public void setPctGrassi(Double pctGrassi) {
		this.pctGrassi = pctGrassi;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
