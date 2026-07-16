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

	/**
	 * F-D1a — decisione consapevole sui conflitti clinici GRAVI col paziente. Se ci sono alimenti gravi
	 * e questo flag non è {@code true}, il backend blocca (409) e restituisce la lista da risolvere.
	 */
	private Boolean confermaConflittiClinici;

	/** F-D1a — id degli alimenti gravi inclusi consapevolmente; gli altri gravi vengono saltati. */
	private List<Long> alimentiForzatiIds;

	public String getMode() { return mode; }
	public void setMode(String mode) { this.mode = mode; }

	public List<RisoluzioneConflittoDto> getRisoluzioni() { return risoluzioni; }
	public void setRisoluzioni(List<RisoluzioneConflittoDto> risoluzioni) { this.risoluzioni = risoluzioni; }

	public Boolean getConfermaConflittiClinici() { return confermaConflittiClinici; }
	public void setConfermaConflittiClinici(Boolean confermaConflittiClinici) { this.confermaConflittiClinici = confermaConflittiClinici; }

	public List<Long> getAlimentiForzatiIds() { return alimentiForzatiIds; }
	public void setAlimentiForzatiIds(List<Long> alimentiForzatiIds) { this.alimentiForzatiIds = alimentiForzatiIds; }
}
