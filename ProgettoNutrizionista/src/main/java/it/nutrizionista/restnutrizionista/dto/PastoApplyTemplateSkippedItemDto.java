package it.nutrizionista.restnutrizionista.dto;

public class PastoApplyTemplateSkippedItemDto {
	private String type;
	private Long alimentoId;
	private Long alternativaId;
	private String message;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getAlimentoId() {
		return alimentoId;
	}

	public void setAlimentoId(Long alimentoId) {
		this.alimentoId = alimentoId;
	}

	public Long getAlternativaId() {
		return alternativaId;
	}

	public void setAlternativaId(Long alternativaId) {
		this.alternativaId = alternativaId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
