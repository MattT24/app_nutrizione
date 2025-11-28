package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalTime;

import it.nutrizionista.restnutrizionista.entity.NomePasto;
import jakarta.validation.constraints.NotBlank;


public class PastoFormDto {

    private Long id;
    @NotBlank(message = "Il nome è obbligatorio")
    private NomePasto nome;
    @NotBlank(message = "La scheda è obbligatoria")
    private SchedaDto scheda;
    //secondo me gli orari sono opzionali
    private LocalTime orarioInizio;
    private LocalTime orarioFine;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public NomePasto getNome() {
		return nome;
	}
	public void setNome(NomePasto nome) {
		this.nome = nome;
	}
	public SchedaDto getScheda() {
		return scheda;
	}
	public void setScheda(SchedaDto scheda) {
		this.scheda = scheda;
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
