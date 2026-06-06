package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO per l'upsert in blocco (bulk diff) delle alternative di un alimento nel
 * pasto template. A differenza del FormDto granulare include {@code id}:
 * <ul>
 *   <li>{@code id == null} → nuova alternativa (INSERT)</li>
 *   <li>{@code id} valorizzato → alternativa esistente (UPDATE)</li>
 * </ul>
 * Le alternative esistenti il cui id non compare nella lista inviata vengono
 * eliminate. Il {@code nomeCustom} resta gestito dagli endpoint display-name.
 */
public record AlimentoSchedaTemplateAlternativaUpsertDto(
        Long id,
        @NotNull Long alimentoAlternativoId,
        @Min(1) Integer quantita,
        Integer priorita,
        String mode,
        Boolean manual,
        String note
) {}
