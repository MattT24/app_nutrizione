package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;

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

import org.hibernate.annotations.BatchSize;
 
@Entity
@Table(name = "macro")
@EntityListeners(AuditingEntityListener.class)
@BatchSize(size = 50)
public class Macro {
 
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@OneToOne(optional = false)
	@JoinColumn(name = "alimento_base_id", nullable = false, unique = true)
	private AlimentoBase alimento;
   
	@Column(nullable = false)
	private Double calorie;
    @Column(nullable = false)
    private Double grassi;
    @Column(nullable = false)
    private Double proteine;
    @Column(nullable = false)
    private Double carboidrati;

	@Column
    private Double fibre;

    @Column
    private Double zuccheri;

    @Column(name = "grassi_saturi")
    private Double grassiSaturi;

    @Column
    private Double sodio;

    @Column
    private Double alcol;

    @Column
    private Double acqua;

    // ── Integrazione OpenFoodFacts: sale etichettato + nutrienti estesi ──
    /** Sale etichettato (g/100g) da OFF (salt_100g). Null per CREA/manuali → si usa getSaleEffettivo(). */
    @Column
    private Double sale;

    @Column(name = "energia_kj")
    private Double energiaKj;

    @Column(name = "zuccheri_aggiunti")
    private Double zuccheriAggiunti;

    @Column(name = "grassi_trans")
    private Double grassiTrans;

    @Column
    private Double colesterolo;

    @CreatedDate
    @Column(nullable = false) 
    private Instant createdAt;
    @LastModifiedDate
    @Column(nullable = false) 
    private Instant updatedAt;

	/**
     * Campo calcolato virtuale per il Frontend.
     * Non viene salvato nel DB, ma serve per visualizzare il Sale.
     * Formula: Sale (g) = Sodio (mg) * 2.5 / 1000
     */
    public Double getSale() {
        if (this.sodio == null) return 0.0;
        // Arrotondamento a 2 decimali per pulizia
        double sale = (this.sodio * 2.5) / 1000.0;
        return Math.round(sale * 100.0) / 100.0;
    }

    /**
     * Sale "effettivo" (g/100g): preferisce il sale etichettato OFF ({@code sale}),
     * altrimenti lo deriva dal sodio ({@code sodio * 2.5 / 1000}). Punto di calcolo UNICO
     * da usare ovunque (mapper + regole cliniche) per evitare divergenze.
     * Null-preserving: ritorna null se né sale né sodio sono dichiarati (≠ getSale() che ritorna 0.0).
     */
    public Double getSaleEffettivo() {
        if (this.sale != null) return this.sale;
        if (this.sodio == null) return null;
        double s = (this.sodio * 2.5) / 1000.0;
        return Math.round(s * 100.0) / 100.0;
    }

    public void setSale(Double sale) { this.sale = sale; }

    public Double getEnergiaKj() { return energiaKj; }
    public void setEnergiaKj(Double energiaKj) { this.energiaKj = energiaKj; }

    public Double getZuccheriAggiunti() { return zuccheriAggiunti; }
    public void setZuccheriAggiunti(Double zuccheriAggiunti) { this.zuccheriAggiunti = zuccheriAggiunti; }

    public Double getGrassiTrans() { return grassiTrans; }
    public void setGrassiTrans(Double grassiTrans) { this.grassiTrans = grassiTrans; }

    public Double getColesterolo() { return colesterolo; }
    public void setColesterolo(Double colesterolo) { this.colesterolo = colesterolo; }

    public Double getFibre() {
		return fibre;
	}
	public void setFibre(Double fibre) {
		this.fibre = fibre;
	}
	public Double getZuccheri() {
		return zuccheri;
	}
	public void setZuccheri(Double zuccheri) {
		this.zuccheri = zuccheri;
	}
	public Double getGrassiSaturi() {
		return grassiSaturi;
	}
	public void setGrassiSaturi(Double grassiSaturi) {
		this.grassiSaturi = grassiSaturi;
	}
	public Double getSodio() {
		return sodio;
	}
	public void setSodio(Double sodio) {
		this.sodio = sodio;
	}
	public Double getAlcol() {
		return alcol;
	}
	public void setAlcol(Double alcol) {
		this.alcol = alcol;
	}
	public Double getAcqua() {
		return acqua;
	}
	public void setAcqua(Double acqua) {
		this.acqua = acqua;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Double getCalorie() {
		return calorie;
	}
	public void setCalorie(Double calorie) {
		this.calorie = calorie;
	}
	public Double getGrassi() {
		return grassi;
	}
	public void setGrassi(Double grassi) {
		this.grassi = grassi;
	}
	
	public AlimentoBase getAlimento() {
		return alimento;
	}
	public void setAlimento(AlimentoBase alimento) {
		this.alimento = alimento;
	}
	public Double getProteine() {
		return proteine;
	}
	public void setProteine(Double proteine) {
		this.proteine = proteine;
	}
	public Double getCarboidrati() {
		return carboidrati;
	}
	public void setCarboidrati(Double carboidrati) {
		this.carboidrati = carboidrati;
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
