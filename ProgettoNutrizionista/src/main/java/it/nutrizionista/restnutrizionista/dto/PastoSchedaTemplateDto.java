package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

public class PastoSchedaTemplateDto {
	private Long id;
	private String nome;
	private String descrizione;
	private String giorno;
	private Integer ordineVisualizzazione;
	private String orarioInizio;
	private String orarioFine;
	private List<AlimentoPastoSchedaTemplateDto> alimentiPasto;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }
	public String getDescrizione() { return descrizione; }
	public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
	public String getGiorno() { return giorno; }
	public void setGiorno(String giorno) { this.giorno = giorno; }
	public Integer getOrdineVisualizzazione() { return ordineVisualizzazione; }
	public void setOrdineVisualizzazione(Integer ordineVisualizzazione) { this.ordineVisualizzazione = ordineVisualizzazione; }
	public String getOrarioInizio() { return orarioInizio; }
	public void setOrarioInizio(String orarioInizio) { this.orarioInizio = orarioInizio; }
	public String getOrarioFine() { return orarioFine; }
	public void setOrarioFine(String orarioFine) { this.orarioFine = orarioFine; }
	public List<AlimentoPastoSchedaTemplateDto> getAlimentiPasto() { return alimentiPasto; }
	public void setAlimentiPasto(List<AlimentoPastoSchedaTemplateDto> alimentiPasto) { this.alimentiPasto = alimentiPasto; }
}
