package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plicometrie")
@EntityListeners(AuditingEntityListener.class)
public class Plicometria {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name = "data_misurazione")
	private LocalDate dataMisurazione;
	
    // Plicometria Jackson & Pollock
	@Column(name = "plica_tricipite")
    private Double plicaTricipite;
	
	@Column(name = "plica_sottoscapolare")
    private Double plicaSottoscapolare;
	
	@Column(name = "plica_pettorale")
    private Double plicaPettorale;
	
	@Column(name = "plica_ascellare_media")
    private Double plicaAscellareMedia;
	
	@Column(name = "plica_sovrailiaca")
    private Double plicaSovrailiaca;
	
	@Column(name = "plica_addominale")
    private Double plicaAddominale;
	
	@Column(name = "plica_coscia")
    private Double plicaCoscia;

    // Metodo usato , farei un enum perchè sono solo 3: 3, 4 o 7 pliche
	@Column(nullable = false)
    private String metodo;

    /*
    // Risultati calcolati, servira calcolarli nel service, lascio come segno, poi eliminiamo
    private Double sommaPliche;
    private Double densitaCorporea;
    private Double percentualeGrasso;
    private Double massaGrassaKg;
    private Double massaMagraKg;
	*/
    @CreatedDate
    @Column(nullable = false) 
    private Instant createdAt;
    @LastModifiedDate
    @Column(nullable = false) 
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
	public String getMetodo() {
		return metodo;
	}
	public void setMetodo(String metodo) {
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
