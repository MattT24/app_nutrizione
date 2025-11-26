package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

public class SchedaDto {

	private Long id;
	private Cliente cliente;
    private Boolean attiva;
    private List<PastoDto> pasti;
    private Instant createdAt; 
    private Instant updatedAt;
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
    
}
