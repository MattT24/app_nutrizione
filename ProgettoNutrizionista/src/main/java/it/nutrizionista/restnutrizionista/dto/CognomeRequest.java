package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

/** DTO semplice per GET con nome nel body. */
public class CognomeRequest {
	
    @NotNull(message = "Il cognome è obbligatorio")
    private String cognome;

    public CognomeRequest() {}
    public CognomeRequest(String cognome) { this.cognome = cognome; }
	
    public String getCognome() {
		return cognome;
	}
	public void setCognome(String cognome) {
		this.cognome = cognome;
	}
    
    
}