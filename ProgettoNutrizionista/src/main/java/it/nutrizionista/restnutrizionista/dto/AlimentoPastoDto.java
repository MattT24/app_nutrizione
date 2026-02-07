package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;



public class AlimentoPastoDto {

	private Long id;
    private AlimentoBaseDto alimento;
    private PastoDto pasto;
    private int quantita;
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

