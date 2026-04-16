package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

public class AlimentoPastoSchedaTemplateDto {
	private Long id;
	private AlimentoBaseDto alimento;
	private int quantita;
	private String nomeCustom;
	private String nomeVisualizzato;
	private List<AlimentoSchedaTemplateAlternativaDto> alternative = new ArrayList<>();

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public AlimentoBaseDto getAlimento() { return alimento; }
	public void setAlimento(AlimentoBaseDto alimento) { this.alimento = alimento; }
	public int getQuantita() { return quantita; }
	public void setQuantita(int quantita) { this.quantita = quantita; }
	public String getNomeCustom() { return nomeCustom; }
	public void setNomeCustom(String nomeCustom) { this.nomeCustom = nomeCustom; }
	public String getNomeVisualizzato() { return nomeVisualizzato; }
	public void setNomeVisualizzato(String nomeVisualizzato) { this.nomeVisualizzato = nomeVisualizzato; }
	public List<AlimentoSchedaTemplateAlternativaDto> getAlternative() { return alternative; }
	public void setAlternative(List<AlimentoSchedaTemplateAlternativaDto> alternative) { this.alternative = alternative; }
}
