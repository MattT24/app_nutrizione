package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;


public class SchedaDto {

	private Long id;
	private ClienteDto cliente;
	private String nome;
    private Boolean attiva;
    private List<PastoDto> pasti;
    private LocalDate dataCreazione;
    private Instant createdAt; 
    private Instant updatedAt;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getAttiva() {
		return attiva;
	}
	public void setAttiva(Boolean attiva) {
		this.attiva = attiva;
	}
	public List<PastoDto> getPasti() {
		return pasti;
	}
	public void setPasti(List<PastoDto> pasti) {
		this.pasti = pasti;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
	public Instant getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
	public ClienteDto getCliente() {
		return cliente;
	}
	public void setCliente(ClienteDto cliente) {
		this.cliente = cliente;
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
