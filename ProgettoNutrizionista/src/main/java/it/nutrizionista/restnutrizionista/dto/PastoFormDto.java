package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;


public class PastoFormDto {

    private Long id;
    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;
    @NotNull(message = "La scheda è obbligatoria")
    private SchedaDto scheda;
    private String descrizione;
    private Integer ordineVisualizzazione;
    //secondo me gli orari sono opzionali
    private LocalTime orarioInizio;
    private LocalTime orarioFine;
    
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
	public SchedaDto getScheda() {
		return scheda;
	}
	public void setScheda(SchedaDto scheda) {
		this.scheda = scheda;
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
	public LocalTime getOrarioInizio() {
		return orarioInizio;
	}
	public void setOrarioInizio(LocalTime orarioInizio) {
		this.orarioInizio = orarioInizio;
	}
	public LocalTime getOrarioFine() {
		return orarioFine;
	}
	public void setOrarioFine(LocalTime orarioFine) {
		this.orarioFine = orarioFine;
	}
    
    
}
