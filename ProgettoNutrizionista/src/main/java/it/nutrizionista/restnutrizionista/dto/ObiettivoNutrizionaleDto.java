package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.entity.TipoObiettivo;

public class ObiettivoNutrizionaleDto {

	private Long id;
	private Long clienteId;
	private TipoObiettivo obiettivo;

	private Double bmr;
	private Double tdee;
	private Double laf;

	private Double targetCalorie;
	private Double targetProteine;
	private Double targetCarboidrati;
	private Double targetGrassi;
	private Double targetFibre;

	private Double pctProteine;
	private Double pctCarboidrati;
	private Double pctGrassi;

	private String note;

	private Instant createdAt;
	private Instant updatedAt;

	// Getters & Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public TipoObiettivo getObiettivo() {
		return obiettivo;
	}

	public void setObiettivo(TipoObiettivo obiettivo) {
		this.obiettivo = obiettivo;
	}

	public Double getBmr() {
		return bmr;
	}

	public void setBmr(Double bmr) {
		this.bmr = bmr;
	}

	public Double getTdee() {
		return tdee;
	}

	public void setTdee(Double tdee) {
		this.tdee = tdee;
	}

	public Double getLaf() {
		return laf;
	}

	public void setLaf(Double laf) {
		this.laf = laf;
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
