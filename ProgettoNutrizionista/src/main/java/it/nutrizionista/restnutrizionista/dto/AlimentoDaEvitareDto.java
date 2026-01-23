package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.entity.TipoRestrizione;


public class AlimentoDaEvitareDto {

    private Long id;
	private AlimentoBaseDto alimento;
	private ClienteDto cliente;
	private TipoRestrizione tipo; 
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
    
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
	public TipoRestrizione getTipo() {
		return tipo;
	}
	public void setTipo(TipoRestrizione tipo) {
		this.tipo = tipo;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
    
    
}
