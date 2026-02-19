package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "obiettivi_nutrizionali")
@EntityListeners(AuditingEntityListener.class)
public class ObiettivoNutrizionale {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "cliente_id", nullable = false, unique = true)
	private Cliente cliente;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoObiettivo obiettivo = TipoObiettivo.MANTENIMENTO;

	// Valori calcolati
	private Double bmr;
	private Double tdee;
	private Double laf;

	// Target macro (g/giorno)
	@Column(name = "target_calorie")
	private Double targetCalorie;
	@Column(name = "target_proteine")
	private Double targetProteine;
	@Column(name = "target_carboidrati")
	private Double targetCarboidrati;
	@Column(name = "target_grassi")
	private Double targetGrassi;
	@Column(name = "target_fibre")
	private Double targetFibre;

	// Percentuali distribuzione
	@Column(name = "pct_proteine")
	private Double pctProteine;
	@Column(name = "pct_carboidrati")
	private Double pctCarboidrati;
	@Column(name = "pct_grassi")
	private Double pctGrassi;

	@Column(columnDefinition = "TEXT")
	private String note;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	// Getters & Setters
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

	public TipoObiettivo getObiettivo() {
		return obiettivo;
	}

	public void setObiettivo(TipoObiettivo obiettivo) {
		this.obiettivo = obiettivo;
	}

	public Double getBmr() {
		return bmr;
	}

	public void setBmr(Double bmr) {
		this.bmr = bmr;
	}

	public Double getTdee() {
		return tdee;
	}

	public void setTdee(Double tdee) {
		this.tdee = tdee;
	}

	public Double getLaf() {
		return laf;
	}

	public void setLaf(Double laf) {
		this.laf = laf;
	}

	public Double getTargetCalorie() {
		return targetCalorie;
	}

	public void setTargetCalorie(Double targetCalorie) {
		this.targetCalorie = targetCalorie;
	}

	public Double getTargetProteine() {
		return targetProteine;
	}

	public void setTargetProteine(Double targetProteine) {
		this.targetProteine = targetProteine;
	}

	public Double getTargetCarboidrati() {
		return targetCarboidrati;
	}

	public void setTargetCarboidrati(Double targetCarboidrati) {
		this.targetCarboidrati = targetCarboidrati;
	}

	public Double getTargetGrassi() {
		return targetGrassi;
	}

	public void setTargetGrassi(Double targetGrassi) {
		this.targetGrassi = targetGrassi;
	}

	public Double getTargetFibre() {
		return targetFibre;
	}

	public void setTargetFibre(Double targetFibre) {
		this.targetFibre = targetFibre;
	}

	public Double getPctProteine() {
		return pctProteine;
	}

	public void setPctProteine(Double pctProteine) {
		this.pctProteine = pctProteine;
	}

	public Double getPctCarboidrati() {
		return pctCarboidrati;
	}

	public void setPctCarboidrati(Double pctCarboidrati) {
		this.pctCarboidrati = pctCarboidrati;
	}

	public Double getPctGrassi() {
		return pctGrassi;
	}

	public void setPctGrassi(Double pctGrassi) {
		this.pctGrassi = pctGrassi;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
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
