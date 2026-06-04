package it.nutrizionista.restnutrizionista.dto;

/**
 * DTO minimale per il dropdown "crea scheda da template": solo i campi
 * effettivamente usati (id, nome, tipo). Esclude descrizione, numeroPasti
 * e timestamp, non necessari in quel contesto.
 */
public record SchedaTemplateSummaryDto(
        Long id,
        String nome,
        String tipo
) {}
