package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.util.List;

public class PastoTemplateDto {
	private Long id;
	private String nome;
	private String descrizione;
	private List<PastoTemplateItemDto> alimenti;
	private Instant createdAt;
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public List<PastoTemplateItemDto> getAlimenti() {
		return alimenti;
	}

	public void setAlimenti(List<PastoTemplateItemDto> alimenti) {
		this.alimenti = alimenti;
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
