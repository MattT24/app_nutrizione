package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AlimentoAlternativoUpsertDto {

	private Long id;

	@NotNull(message = "L'ID dell'alimento alternativo è obbligatorio")
	private Long alimentoAlternativoId;

	@Min(value = 1, message = "La quantità deve essere almeno 1 grammo")
	private Integer quantita;

	@Min(value = 1, message = "La priorità deve essere almeno 1")
	private Integer priorita;

	private AlternativeMode mode;

	private Boolean manual;

	private String note;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

