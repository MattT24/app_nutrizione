package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO per aggiornare solo i metadata di un template (nome, descrizione, tipo).
 * Sostituisce l'uso di SchedaTemplateUpsertDto per le operazioni PATCH.
 */
public record SchedaTemplateMetadataPatchDto(
	@NotBlank(message = "Il nome del template e' obbligatorio")
	String nome,

	String descrizione,

	@NotNull(message = "Il tipo e' obbligatorio (GIORNALIERA o SETTIMANALE)")
	String tipo
) {}
