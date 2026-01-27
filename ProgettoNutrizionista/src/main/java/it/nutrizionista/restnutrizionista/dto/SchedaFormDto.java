package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SchedaFormDto {

	private Long id;
	@NotBlank(message = "Il nome è obbligatorio")
	private String nome;
    @NotNull(message = "Il cliente è obbligatorio")
	private Cliente cliente;
    //non ho idea dell'annotazione dei boolean
    private Boolean attiva;
    private LocalDate dataCreazione;

    
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
	public LocalDate getDataCreazione() {
		return dataCreazione;
	}
	public void setDataCreazione(LocalDate dataCreazione) {
		this.dataCreazione = dataCreazione;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	
    
}
