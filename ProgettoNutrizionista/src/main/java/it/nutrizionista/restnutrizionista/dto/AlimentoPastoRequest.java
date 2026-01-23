package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

public class AlimentoPastoRequest {

    @NotBlank(message = "L'alimento base è obbligatorio")
    private AlimentoBaseDto alimento;
    @NotBlank(message = "Il pasto è obbligatorio")
    private PastoDto pasto;
    @NotBlank(message = "La quantita è obbligatoria")
    private int quantita;
    
    private boolean forzaInserimento = false; 

    public boolean isForzaInserimento() { return forzaInserimento; }
    public void setForzaInserimento(boolean forzaInserimento) { this.forzaInserimento = forzaInserimento; }

	public AlimentoBaseDto getAlimento() {
		return alimento;
	}
	public void setAlimento(AlimentoBaseDto alimento) {
		this.alimento = alimento;
	}
	public PastoDto getPasto() {
		return pasto;
	}
	public void setPasto(PastoDto pasto) {
		this.pasto = pasto;
	}
	public int getQuantita() {
		return quantita;
	}
	public void setQuantita(int quantita) {
		this.quantita = quantita;
	}
    
    
}
