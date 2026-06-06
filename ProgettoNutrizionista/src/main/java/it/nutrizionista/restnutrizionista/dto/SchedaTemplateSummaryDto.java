package it.nutrizionista.restnutrizionista.dto;

/**
 * DTO leggero per il dropdown "crea scheda da template" e la lista template:
 * id, nome, tipo e numero di pasti. Esclude descrizione, alberi pasti/alimenti
 * e timestamp, non necessari in quei contesti.
 */
public record SchedaTemplateSummaryDto(
        Long id,
        String nome,
        String tipo,
        Integer numeroPasti
) {}
