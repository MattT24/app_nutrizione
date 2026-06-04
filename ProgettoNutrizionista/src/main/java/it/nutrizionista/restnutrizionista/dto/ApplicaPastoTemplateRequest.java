package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Richiesta per applicare un pasto-template a un pasto della scheda-template
 * in un'unica operazione server-side (anziché N chiamate granulari dal client).
 * {@code mode}: "REPLACE" (default) sostituisce gli alimenti esistenti,
 * "MERGE" aggiunge solo gli alimenti non già presenti.
 */
public record ApplicaPastoTemplateRequest(
        @NotNull Long pastoTemplateId,
        String mode
) {}
