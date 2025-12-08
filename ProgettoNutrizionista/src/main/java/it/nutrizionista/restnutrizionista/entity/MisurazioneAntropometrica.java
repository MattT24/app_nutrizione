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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "misurazioni_antropometriche")
@EntityListeners(AuditingEntityListener.class)
public class MisurazioneAntropometrica {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column
	private Double spalle;
	
	@Column
	private Double vita;
	
	@Column
	private Double fianchi;
	
	@Column
	private Double torace;

	@Column(name = "gamba_sinistra")
	private Double gambaS;
	
	@Column(name = "gamba_destra")
	private Double gambaD;
	
	@Column(name = "bicipite_sinistro")
	private Double bicipiteS;
	
	@Column(name = "bicipite_destro")
	private Double bicipiteD;
	
	@Column(name = "data_misurazione")
	private LocalDate dataMisurazione;
	
	@OneToOne
    @JoinColumn(name = "cliente_id")
	private Cliente cliente;
	
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

	public Double getSpalle() {
		return spalle;
	}

	public void setSpalle(Double spalle) {
		this.spalle = spalle;
	}

	public Double getVita() {
		return vita;
	}

	public void setVita(Double vita) {
		this.vita = vita;
	}

	public Double getFianchi() {
		return fianchi;
	}

	public void setFianchi(Double fianchi) {
		this.fianchi = fianchi;
	}

	public Double getTorace() {
		return torace;
	}

	public void setTorace(Double torace) {
		this.torace = torace;
	}

	public Double getGambaS() {
		return gambaS;
	}

	public void setGambaS(Double gambaS) {
		this.gambaS = gambaS;
	}

	public Double getGambaD() {
		return gambaD;
	}

	public void setGambaD(Double gambaD) {
		this.gambaD = gambaD;
	}

	public Double getBicipiteS() {
		return bicipiteS;
	}

	public void setBicipiteS(Double bicipiteS) {
		this.bicipiteS = bicipiteS;
	}

	public Double getBicipiteD() {
		return bicipiteD;
	}

	public void setBicipiteD(Double bicipiteD) {
		this.bicipiteD = bicipiteD;
	}

	public LocalDate getDataMisurazione() {
		return dataMisurazione;
	}

	public void setDataMisurazione(LocalDate dataMisurazione) {
		this.dataMisurazione = dataMisurazione;
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

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	

	
}