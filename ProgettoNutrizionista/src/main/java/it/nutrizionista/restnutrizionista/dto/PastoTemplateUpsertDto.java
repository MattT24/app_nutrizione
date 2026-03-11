package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class PastoTemplateUpsertDto {
	@NotBlank(message = "Il nome del template è obbligatorio")
	private String nome;

	private String descrizione;

	@Valid
	private List<PastoTemplateItemUpsertDto> alimenti = new ArrayList<>();

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

	public List<PastoTemplateItemUpsertDto> getAlimenti() {
		return alimenti;
	}

	public void setAlimenti(List<PastoTemplateItemUpsertDto> alimenti) {
		this.alimenti = alimenti;
	}
}
