package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.AlternativeMode;

public class PastoTemplateAlternativaDto {
	private Long id;
	private AlimentoBaseDto alimentoAlternativo;
	private Integer quantita;
	private Integer priorita;
	private AlternativeMode mode;
	private Boolean manual;
	private String note;
	private String nomeCustom;
	private String nomeVisualizzato;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getNomeCustom() {
		return nomeCustom;
	}

	public void setNomeCustom(String nomeCustom) {
		this.nomeCustom = nomeCustom;
	}

	public String getNomeVisualizzato() {
		return nomeVisualizzato;
	}

	public void setNomeVisualizzato(String nomeVisualizzato) {
		this.nomeVisualizzato = nomeVisualizzato;
	}
}
