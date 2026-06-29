package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class ApplicaSchedaTemplateRequest {
	@NotNull(message = "La modalita e' obbligatoria (REPLACE o MERGE)")
	private String mode;

	/**
	 * Decisioni sui pasti in conflitto (solo MERGE). Se assenti/vuote e ci sono conflitti,
	 * il backend non applica nulla e restituisce l'elenco dei conflitti da risolvere.
	 */
	private List<RisoluzioneConflittoDto> risoluzioni;

	public String getMode() { return mode; }
	public void setMode(String mode) { this.mode = mode; }

	public List<RisoluzioneConflittoDto> getRisoluzioni() { return risoluzioni; }
	public void setRisoluzioni(List<RisoluzioneConflittoDto> risoluzioni) { this.risoluzioni = risoluzioni; }
}
