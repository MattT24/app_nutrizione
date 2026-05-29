package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AlimentoPastoSchedaTemplateUpsertDto {
	@NotNull(message = "L'id alimento e' obbligatorio")
	private Long alimentoId;
	@Min(value = 1, message = "La quantita deve essere almeno 1")
	private int quantita = 100;
	private String nomeCustom;

	public Long getAlimentoId() { return alimentoId; }
	public void setAlimentoId(Long alimentoId) { this.alimentoId = alimentoId; }
	public int getQuantita() { return quantita; }
	public void setQuantita(int quantita) { this.quantita = quantita; }
	public String getNomeCustom() { return nomeCustom; }
	public void setNomeCustom(String nomeCustom) { this.nomeCustom = nomeCustom; }
}
