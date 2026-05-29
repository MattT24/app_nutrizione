package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO per aggiornare quantita'/nomeCustom di un alimento in un pasto template.
 */
public record AlimentoPastoSchedaTemplatePatchDto(
	@NotNull(message = "La quantita' e' obbligatoria")
	@Min(value = 1, message = "La quantita' deve essere almeno 1 grammo")
	Integer quantita,

	String nomeCustom
) {}
