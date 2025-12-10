package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

import it.nutrizionista.restnutrizionista.entity.Metodo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class PlicometriaFormDto {

	private Long id;
	@NotBlank(message = "Il metodo da utilizzare è obbligatorio")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Metodo metodo;
	@NotBlank(message = "La data è obbligatoria")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private LocalDate dataMisurazione;
	@Positive(message = "La plica deve essere maggiore di 0")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Double plicaTricipite;
	@Positive(message = "La plica deve essere maggiore di 0")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Double plicaSottoscapolare;
	@Positive(message = "La plica deve essere maggiore di 0")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Double plicaPettorale;
	@Positive(message = "La plica deve essere maggiore di 0")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Double plicaAscellareMedia;
	@Positive(message = "La plica deve essere maggiore di 0")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Double plicaSovrailiaca;
	@Positive(message = "La plica deve essere maggiore di 0")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Double plicaAddominale;
	@Positive(message = "La plica deve essere maggiore di 0")
	@Max(value = 150, message = "Una plica non può superare 150mm")
	private Double plicaCoscia;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Metodo getMetodo() {
		return metodo;
	}
	public void setMetodo(Metodo metodo) {
		this.metodo = metodo;
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

	
	
	
}
