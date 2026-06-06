package it.nutrizionista.restnutrizionista.dto;

/**
 * DTO leggero per la lista delle Schede Template.
 * Contiene solo i metadati necessari alla vista card,
 * escludendo l'intero albero pasti/alimenti/alternative.
 */
public record SchedaTemplateListDto(
	Long id,
	String nome,
	String descrizione,
	String tipo,
	Integer numeroPasti
) {}
