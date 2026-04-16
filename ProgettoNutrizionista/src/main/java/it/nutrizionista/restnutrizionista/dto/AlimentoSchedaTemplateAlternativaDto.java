package it.nutrizionista.restnutrizionista.dto;

/**
 * DTO in uscita per un AlimentoSchedaTemplateAlternativa (con ID).
 */
public record AlimentoSchedaTemplateAlternativaDto(
	Long id,
	Long alimentoPastoSchedaTemplateId,
	AlimentoBaseDto alimentoAlternativo,
	Integer quantita,
	Integer priorita,
	String mode,
	Boolean manual,
	String nomeCustom,
	String nomeVisualizzato
) {}
