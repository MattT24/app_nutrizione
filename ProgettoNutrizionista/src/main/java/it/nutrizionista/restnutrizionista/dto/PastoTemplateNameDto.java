package it.nutrizionista.restnutrizionista.dto;

/**
 * DTO minimale per il dropdown di selezione dei template-pasto.
 *
 * Versione leggera di {@link PastoTemplateDto}: trasporta solo {@code id} e
 * {@code nome}, senza l'intero albero (alimenti, alternative, macroNutrienti)
 * che il dropdown non usa. Caricato via query di proiezione, senza fetch join.
 */
public record PastoTemplateNameDto(Long id, String nome) {}
