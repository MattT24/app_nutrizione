package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

public class AlimentoDaEvitareFormDto {

    private Long id;
    @NotBlank(message = "L'alimento base è obbligatorio")
	private AlimentoBaseDto alimento;
    @NotBlank(message = "Il cliente è obbligatorio")
	private ClienteDto cliente;

    
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
	public ClienteDto getCliente() {
		return cliente;
	}
	public void setCliente(ClienteDto cliente) {
		this.cliente = cliente;
	}

    
    
}
