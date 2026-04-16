package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

public class ApplicaSchedaTemplateRequest {
	@NotNull(message = "La modalita e' obbligatoria (REPLACE o MERGE)")
	private String mode;

	public String getMode() { return mode; }
	public void setMode(String mode) { this.mode = mode; }
}
