package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "plicometrie")
@EntityListeners(AuditingEntityListener.class)
public class Plicometria {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name = "data_misurazione")
	private LocalDate dataMisurazione;
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;
	private Double tricipite;
    private Double bicipite;      // Nuovo (usato in Durnin)
    private Double sottoscapolare;
    private Double sovrailiaca;
    private Double addominale;
    private Double coscia;
    private Double pettorale;     // Nuovo (usato in JP3 Uomo)
    private Double ascellare;     // Nuovo (usato in JP7)
    private Double polpaccio;     // Nuovo (usato in altri metodi)
    
    private Double percentualeMassaGrassa;


    private Double pesoKgRiferimento;   // peso usato nel calcolo (da Cliente)
    private Double sommaPliche;         // utile per debug/grafici
    private Double densitaCorporea;     // Body Density
    private Double massaGrassaKg;
    private Double massaMagraKg;
    private String note;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Metodo metodo;

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

	public Double getTricipite() {
		return tricipite;
	}
	public void setTricipite(Double tricipite) {
		this.tricipite = tricipite;
	}
	public Double getBicipite() {
		return bicipite;
	}
	public void setBicipite(Double bicipite) {
		this.bicipite = bicipite;
	}
	public Double getSottoscapolare() {
		return sottoscapolare;
	}
	public void setSottoscapolare(Double sottoscapolare) {
		this.sottoscapolare = sottoscapolare;
	}
	public Double getSovrailiaca() {
		return sovrailiaca;
	}
	public void setSovrailiaca(Double sovrailiaca) {
		this.sovrailiaca = sovrailiaca;
	}
	public Double getAddominale() {
		return addominale;
	}
	public void setAddominale(Double addominale) {
		this.addominale = addominale;
	}
	public Double getCoscia() {
		return coscia;
	}
	public void setCoscia(Double coscia) {
		this.coscia = coscia;
	}
	public Double getPettorale() {
		return pettorale;
	}
	public void setPettorale(Double pettorale) {
		this.pettorale = pettorale;
	}
	public Double getAscellare() {
		return ascellare;
	}
	public void setAscellare(Double ascellare) {
		this.ascellare = ascellare;
	}
	public Double getPolpaccio() {
		return polpaccio;
	}
	public void setPolpaccio(Double polpaccio) {
		this.polpaccio = polpaccio;
	}

	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
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
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	public Double getPesoKgRiferimento() {
		return pesoKgRiferimento;
	}
	public void setPesoKgRiferimento(Double pesoKgRiferimento) {
		this.pesoKgRiferimento = pesoKgRiferimento;
	}
	public Double getSommaPliche() {
		return sommaPliche;
	}
	public void setSommaPliche(Double sommaPliche) {
		this.sommaPliche = sommaPliche;
	}
	public Double getDensitaCorporea() {
		return densitaCorporea;
	}
	public void setDensitaCorporea(Double densitaCorporea) {
		this.densitaCorporea = densitaCorporea;
	}
	public Double getMassaGrassaKg() {
		return massaGrassaKg;
	}
	public void setMassaGrassaKg(Double massaGrassaKg) {
		this.massaGrassaKg = massaGrassaKg;
	}
	public Double getMassaMagraKg() {
		return massaMagraKg;
	}
	public void setMassaMagraKg(Double massaMagraKg) {
		this.massaMagraKg = massaMagraKg;
	}
	public Double getPercentualeMassaGrassa() {
		return percentualeMassaGrassa;
	}
	public void setPercentualeMassaGrassa(Double percentualeMassaGrassa) {
		this.percentualeMassaGrassa = percentualeMassaGrassa;
	}

    
}
