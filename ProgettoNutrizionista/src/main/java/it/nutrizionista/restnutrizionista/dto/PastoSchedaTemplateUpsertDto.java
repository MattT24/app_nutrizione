package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class PastoSchedaTemplateUpsertDto {
	@NotBlank(message = "Il nome del pasto e' obbligatorio")
	private String nome;
	private String descrizione;
	private String giorno;
	private Integer ordineVisualizzazione;
	private String orarioInizio;
	private String orarioFine;
	@Valid
	private List<AlimentoPastoSchedaTemplateUpsertDto> alimentiPasto = new ArrayList<>();

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
	public List<AlimentoPastoSchedaTemplateUpsertDto> getAlimentiPasto() { return alimentiPasto; }
	public void setAlimentiPasto(List<AlimentoPastoSchedaTemplateUpsertDto> alimentiPasto) { this.alimentiPasto = alimentiPasto; }
}
