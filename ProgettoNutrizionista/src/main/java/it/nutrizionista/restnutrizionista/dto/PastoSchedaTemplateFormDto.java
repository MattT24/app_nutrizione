package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO per creare/aggiornare un pasto nel template (operazioni granulari).
 */
public record PastoSchedaTemplateFormDto(
	@NotBlank(message = "Il nome del pasto e' obbligatorio")
	String nome,

	String descrizione,
	String giorno,
	String orarioInizio,
	String orarioFine
) {}
