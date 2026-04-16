package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO per aggiungere un alimento dal catalogo a un pasto nel template.
 */
public record AlimentoPastoSchedaTemplateCreateDto(
	@NotNull(message = "L'ID dell'alimento e' obbligatorio")
	Long alimentoId,

	@NotNull(message = "La quantita' e' obbligatoria")
	@Min(value = 1, message = "La quantita' deve essere almeno 1 grammo")
	Integer quantita,

	String nomeCustom
) {}
