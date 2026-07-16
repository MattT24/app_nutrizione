package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

public class PastoApplyTemplateResultDto {
	private PastoDto pasto;
	private PastoApplyTemplateStatsDto stats = new PastoApplyTemplateStatsDto();
	private List<PastoApplyTemplateSkippedItemDto> skipped = new ArrayList<>();
	/**
	 * F-D1a — conflitti clinici (WARNING + ALERT_GRAVE) degli alimenti del template contro il paziente,
	 * strutturati per il modale FE (gravi con toggle + allergeni distinti; warning come info). Evita al FE
	 * di parsare i {@code message} dello {@code skipped[]}.
	 */
	private List<ConflittoClinicoDto> conflittiClinici = new ArrayList<>();

	public PastoDto getPasto() {
		return pasto;
	}

	public void setPasto(PastoDto pasto) {
		this.pasto = pasto;
	}

	public PastoApplyTemplateStatsDto getStats() {
		return stats;
	}

	public void setStats(PastoApplyTemplateStatsDto stats) {
		this.stats = stats;
	}

	public List<PastoApplyTemplateSkippedItemDto> getSkipped() {
		return skipped;
	}

	public void setSkipped(List<PastoApplyTemplateSkippedItemDto> skipped) {
		this.skipped = skipped;
	}

	public List<ConflittoClinicoDto> getConflittiClinici() {
		return conflittiClinici;
	}

	public void setConflittiClinici(List<ConflittoClinicoDto> conflittiClinici) {
		this.conflittiClinici = conflittiClinici;
	}
}
