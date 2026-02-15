package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
 
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "pasti")
@EntityListeners(AuditingEntityListener.class)
public class Pasto {
 
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false)
    private String nome;
    
    @Column(name = "default_code")
    private String defaultCode;
    
    @Column(columnDefinition = "TEXT")
    private String descrizione;
    
    @Column(name = "ordine_visualizzazione", nullable = false)
    private Integer ordineVisualizzazione = 0;
    
    @Column(nullable = false)
    private Boolean eliminabile = true;
    
    @ManyToOne
    @JoinColumn(name = "scheda_id")
    private Scheda scheda;
    
    @OneToMany(mappedBy = "pasto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlimentoPasto> alimentiPasto;
    
    @Column(name = "orario_inizio")
    private LocalTime orarioInizio;
    
    @Column(name = "orario_fine")
    private LocalTime orarioFine;
 
    
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
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getDefaultCode() {
		return defaultCode;
	}
	public void setDefaultCode(String defaultCode) {
		this.defaultCode = defaultCode;
	}
	public String getDescrizione() {
		return descrizione;
	}
	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}
	public Integer getOrdineVisualizzazione() {
		return ordineVisualizzazione;
	}
	public void setOrdineVisualizzazione(Integer ordineVisualizzazione) {
		this.ordineVisualizzazione = ordineVisualizzazione;
	}
	public Boolean getEliminabile() {
		return eliminabile;
	}
	public void setEliminabile(Boolean eliminabile) {
		this.eliminabile = eliminabile;
	}
	public List<AlimentoPasto> getAlimentiPasto() {
		return alimentiPasto;
	}
	public void setAlimentiPasto(List<AlimentoPasto> alimentiPasto) {
		this.alimentiPasto = alimentiPasto;
	}
	public LocalTime getOrarioInizio() {
		return orarioInizio;
	}
	public void setOrarioInizio(LocalTime orarioInizio) {
		this.orarioInizio = orarioInizio;
	}
	public LocalTime getOrarioFine() {
		return orarioFine;
	}
	public void setOrarioFine(LocalTime orarioFine) {
		this.orarioFine = orarioFine;
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
	public Scheda getScheda() {
		return scheda;
	}
	public void setScheda(Scheda scheda) {
		this.scheda = scheda;
	}
	
	

}
