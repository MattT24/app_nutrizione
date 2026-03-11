package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PastoTemplateItemUpsertDto {
	@NotNull(message = "L'ID dell'alimento è obbligatorio")
	private Long alimentoId;

	@Min(value = 1, message = "La quantità deve essere almeno 1 grammo")
	private Double quantita;

	private String nomeCustom;

	@Valid
	private List<PastoTemplateAlternativaUpsertDto> alternative = new ArrayList<>();

	public Long getAlimentoId() {
		return alimentoId;
	}

	public void setAlimentoId(Long alimentoId) {
		this.alimentoId = alimentoId;
	}

	public Double getQuantita() {
		return quantita;
	}

	public void setQuantita(Double quantita) {
		this.quantita = quantita;
	}

	public String getNomeCustom() {
		return nomeCustom;
	}

	public void setNomeCustom(String nomeCustom) {
		this.nomeCustom = nomeCustom;
	}

	public List<PastoTemplateAlternativaUpsertDto> getAlternative() {
		return alternative;
	}

	public void setAlternative(List<PastoTemplateAlternativaUpsertDto> alternative) {
		this.alternative = alternative;
	}
}
