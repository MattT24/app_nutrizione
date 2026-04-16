package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO in ingresso per creare/aggiornare un AlimentoSchedaTemplateAlternativa (senza ID).
 */
public record AlimentoSchedaTemplateAlternativaFormDto(
	@NotNull(message = "L'ID dell'alimento alternativo e' obbligatorio")
	Long alimentoAlternativoId,

	@NotNull(message = "La quantita' e' obbligatoria")
	@Min(value = 1, message = "La quantita' deve essere almeno 1 grammo")
	Integer quantita,

	Integer priorita,
	String mode,
	Boolean manual,
	String note
) {}
