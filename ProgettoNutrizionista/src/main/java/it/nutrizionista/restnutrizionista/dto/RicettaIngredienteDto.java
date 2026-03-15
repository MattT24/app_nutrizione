package it.nutrizionista.restnutrizionista.dto;

public class RicettaIngredienteDto {

	private Long id;
	private AlimentoBaseDto alimento;
	private Double quantita;
	private String nomeCustom;
	/** Nome da mostrare in UI: nomeCustom se presente, altrimenti alimento.nome */
	private String nomeVisualizzato;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public AlimentoBaseDto getAlimento() { return alimento; }
	public void setAlimento(AlimentoBaseDto alimento) { this.alimento = alimento; }

	public Double getQuantita() { return quantita; }
	public void setQuantita(Double quantita) { this.quantita = quantita; }

	public String getNomeCustom() { return nomeCustom; }
	public void setNomeCustom(String nomeCustom) { this.nomeCustom = nomeCustom; }

	public String getNomeVisualizzato() { return nomeVisualizzato; }
	public void setNomeVisualizzato(String nomeVisualizzato) { this.nomeVisualizzato = nomeVisualizzato; }
}
