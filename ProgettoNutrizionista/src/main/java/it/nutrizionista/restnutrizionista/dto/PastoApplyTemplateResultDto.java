package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

public class PastoApplyTemplateResultDto {
	private PastoDto pasto;
	private PastoApplyTemplateStatsDto stats = new PastoApplyTemplateStatsDto();
	private List<PastoApplyTemplateSkippedItemDto> skipped = new ArrayList<>();

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
}
