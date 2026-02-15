package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MealCreateRequest {
	@NotNull(message = "La scheda è obbligatoria")
	private Long schedaId;
	
	@NotBlank(message = "Il nome è obbligatorio")
	private String nome;
	
	private String descrizione;
	private Integer ordineVisualizzazione;

	public Long getSchedaId() {
		return schedaId;
	}

	public void setSchedaId(Long schedaId) {
		this.schedaId = schedaId;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public Integer getOrdineVisualizzazione() {
		return ordineVisualizzazione;
	}

	public void setOrdineVisualizzazione(Integer ordineVisualizzazione) {
		this.ordineVisualizzazione = ordineVisualizzazione;
	}
}

