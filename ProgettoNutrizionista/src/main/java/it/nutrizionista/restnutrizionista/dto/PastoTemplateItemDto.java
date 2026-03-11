package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

public class PastoTemplateItemDto {
	private AlimentoBaseDto alimento;
	private Double quantita;
	private String nomeCustom;
	private String nomeVisualizzato;
	private List<PastoTemplateAlternativaDto> alternative;

	public AlimentoBaseDto getAlimento() {
		return alimento;
	}

	public void setAlimento(AlimentoBaseDto alimento) {
		this.alimento = alimento;
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

	public String getNomeVisualizzato() {
		return nomeVisualizzato;
	}

	public void setNomeVisualizzato(String nomeVisualizzato) {
		this.nomeVisualizzato = nomeVisualizzato;
	}

	public List<PastoTemplateAlternativaDto> getAlternative() {
		return alternative;
	}

	public void setAlternative(List<PastoTemplateAlternativaDto> alternative) {
		this.alternative = alternative;
	}
}
