package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alimenti_base")
@EntityListeners(AuditingEntityListener.class)
public class AlimentoBase {
 
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name = "nome", nullable = false, unique = true)
	private String nome;
	
	@OneToMany(mappedBy = "alimento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlimentoPasto> alimentiScelti;
	
	@OneToMany(mappedBy = "alimento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlimentoDaEvitare> alimentiEvitati;
 
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "alimento")
	private Macro macroNutrienti;

    @Column(name = "misura_grammi", nullable = false)
    private Double misuraInGrammi;

    @OneToMany(mappedBy = "alimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ValoreMicro> micronutrienti = new HashSet<>();
 
    
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
	public Macro getMacronutrienti() {
		return macroNutrienti;
	}
	public void setMacronutrienti(Macro macronutrienti) {
		this.macroNutrienti = macronutrienti;
	}
	public Double getMisuraInGrammi() {
		return misuraInGrammi;
	}
	public void setMisuraInGrammi(Double misuraInGrammi) {
		this.misuraInGrammi = misuraInGrammi;
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
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public List<AlimentoPasto> getAlimentiPasto() {
		return alimentiScelti;
	}
	public void setAlimentiPasto(List<AlimentoPasto> alimentiPasto) {
		this.alimentiScelti = alimentiPasto;
	}
	public List<AlimentoDaEvitare> getAlimentiEvitati() {
		return alimentiEvitati;
	}
	public void setAlimentiEvitati(List<AlimentoDaEvitare> alimentiEvitati) {
		this.alimentiEvitati = alimentiEvitati;
	}
	public List<AlimentoPasto> getAlimentiScelti() {
		return alimentiScelti;
	}
	public void setAlimentiScelti(List<AlimentoPasto> alimentiScelti) {
		this.alimentiScelti = alimentiScelti;
	}
	public Macro getMacroNutrienti() {
		return macroNutrienti;
	}
	public void setMacroNutrienti(Macro macroNutrienti) {
		this.macroNutrienti = macroNutrienti;
	}
	public Set<ValoreMicro> getMicronutrienti() {
		return micronutrienti;
	}
	public void setMicronutrienti(Set<ValoreMicro> micronutrienti) {
		this.micronutrienti = micronutrienti;
	}
	
	
 
    
}


