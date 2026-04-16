package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.util.List;

public class SchedaTemplateDto {
	private Long id;
	private String nome;
	private String descrizione;
	private String tipo;
	private List<PastoSchedaTemplateDto> pasti;
	private Integer numeroPasti;
	private Instant createdAt;
	private Instant updatedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }
	public String getDescrizione() { return descrizione; }
	public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public List<PastoSchedaTemplateDto> getPasti() { return pasti; }
	public void setPasti(List<PastoSchedaTemplateDto> pasti) { this.pasti = pasti; }
	public Integer getNumeroPasti() { return numeroPasti; }
	public void setNumeroPasti(Integer numeroPasti) { this.numeroPasti = numeroPasti; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
