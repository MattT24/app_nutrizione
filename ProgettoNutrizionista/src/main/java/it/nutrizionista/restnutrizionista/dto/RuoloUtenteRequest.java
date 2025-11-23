package it.nutrizionista.restnutrizionista.dto;


import jakarta.validation.constraints.NotNull;

public class RuoloUtenteRequest {

    @NotNull(message = "L'id del gruppo è obbligatorio")
    private Long ruoloId;

    @NotNull(message = "La lista dei permessi è obbligatoria")
    private Long utenteId;

	public Long getRuoloId() {
		return ruoloId;
	}

	public void setRuoloId(Long ruoloId) {
		this.ruoloId = ruoloId;
	}

	public Long getUtenteId() {
		return utenteId;
	}

	public void setUtenteId(Long utenteId) {
		this.utenteId = utenteId;
	}
    
    
}
