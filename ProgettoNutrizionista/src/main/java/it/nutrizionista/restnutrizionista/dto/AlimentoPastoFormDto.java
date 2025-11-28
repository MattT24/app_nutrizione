package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

public class AlimentoPastoFormDto {

	private Long id;
    @NotBlank(message = "L'alimento base è obbligatorio")
    private AlimentoBaseDto alimento;
    @NotBlank(message = "Il pasto è obbligatorio")
    private PastoDto pasto;
    @NotBlank(message = "La quantità è obbligatoria")
    private int quantità;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
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
	public int getQuantità() {
		return quantità;
	}
	public void setQuantità(int quantità) {
		this.quantità = quantità;
	}
    
    
}
