package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public class PastoOrariFormDto {
	@NotNull(message = "Orario inizio obbligatorio")
	private LocalTime orarioInizio;
	@NotNull(message = "Orario fine obbligatorio")
	private LocalTime orarioFine;
	
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

