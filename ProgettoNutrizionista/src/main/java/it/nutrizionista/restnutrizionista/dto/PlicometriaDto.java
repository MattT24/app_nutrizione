package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalDate;
import it.nutrizionista.restnutrizionista.entity.Metodo;

public class PlicometriaDto {

	private Long id;
	private LocalDate dataMisurazione;
	private Double plicaTricipite;
	private Double plicaSottoscapolare;
	private Double plicaPettorale;
	private Double plicaAscellareMedia;
	private Double plicaSovrailiaca;
	private Double plicaAddominale;
	private Double plicaCoscia;
	private Metodo metodo;
	private Instant createdAt;
	private Instant updatedAt;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public LocalDate getDataMisurazione() {
		return dataMisurazione;
	}
	public void setDataMisurazione(LocalDate dataMisurazione) {
		this.dataMisurazione = dataMisurazione;
	}
	public Double getPlicaTricipite() {
		return plicaTricipite;
	}
	public void setPlicaTricipite(Double plicaTricipite) {
		this.plicaTricipite = plicaTricipite;
	}
	public Double getPlicaSottoscapolare() {
		return plicaSottoscapolare;
	}
	public void setPlicaSottoscapolare(Double plicaSottoscapolare) {
		this.plicaSottoscapolare = plicaSottoscapolare;
	}
	public Double getPlicaPettorale() {
		return plicaPettorale;
	}
	public void setPlicaPettorale(Double plicaPettorale) {
		this.plicaPettorale = plicaPettorale;
	}
	public Double getPlicaAscellareMedia() {
		return plicaAscellareMedia;
	}
	public void setPlicaAscellareMedia(Double plicaAscellareMedia) {
		this.plicaAscellareMedia = plicaAscellareMedia;
	}
	public Double getPlicaSovrailiaca() {
		return plicaSovrailiaca;
	}
	public void setPlicaSovrailiaca(Double plicaSovrailiaca) {
		this.plicaSovrailiaca = plicaSovrailiaca;
	}
	public Double getPlicaAddominale() {
		return plicaAddominale;
	}
	public void setPlicaAddominale(Double plicaAddominale) {
		this.plicaAddominale = plicaAddominale;
	}
	public Double getPlicaCoscia() {
		return plicaCoscia;
	}
	public void setPlicaCoscia(Double plicaCoscia) {
		this.plicaCoscia = plicaCoscia;
	}
	public Metodo getMetodo() {
		return metodo;
	}
	public void setMetodo(Metodo metodo) {
		this.metodo = metodo;
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
