package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import jakarta.validation.constraints.NotNull;

public class SchedaFormDto {

	private Long id;
    @NotNull(message = "Il cliente è obbligatorio")
	private Cliente cliente;
    //non ho idea dell'annotazione dei boolean
    private Boolean attiva;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	public Boolean getAttiva() {
		return attiva;
	}
	public void setAttiva(Boolean attiva) {
		this.attiva = attiva;
	}
	
    
}
