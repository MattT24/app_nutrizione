package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "alimenti_pasto")
@EntityListeners(AuditingEntityListener.class)
public class AlimentoPasto {
 
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id")
    private AlimentoBase alimento;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasto_id")
    private Pasto pasto;
    
    @Column(nullable = false)
    private int quantita;
    
    /**
     * Lista di alimenti alternativi (sostitutivi) per questo alimento nel pasto
     */
    @OneToMany(mappedBy = "alimentoPasto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AlimentoAlternativo> alternative = new LinkedHashSet<>();

    @OneToOne(mappedBy = "alimentoPasto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AlimentoPastoNomeOverride nomeOverride;
    
    @CreatedDate
    @Column(nullable = false) 
    private Instant createdAt;
    @LastModifiedDate
    @Column(nullable = false) 
    private Instant updatedAt;
    
    public AlimentoPasto(){}
    
    
	public AlimentoPasto(AlimentoBase alimento, Pasto pasto, int quantita) {
		super();
		this.alimento = alimento;
		this.pasto = pasto;
		this.quantita = quantita;
	}


	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public AlimentoBase getAlimento() {
		return alimento;
	}
	public void setAlimento(AlimentoBase alimento) {
		this.alimento = alimento;
	}
	public Pasto getPasto() {
		return pasto;
	}
	public void setPasto(Pasto pasto) {
		this.pasto = pasto;
	}
	public int getQuantita() {
		return quantita;
	}
	public void setQuantita(int quantita) {
		this.quantita = quantita;
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
	
	public Set<AlimentoAlternativo> getAlternative() {
		return alternative;
	}
	public void setAlternative(Set<AlimentoAlternativo> alternative) {
		this.alternative = alternative;
	}
	public AlimentoPastoNomeOverride getNomeOverride() {
		return nomeOverride;
	}
	public void setNomeOverride(AlimentoPastoNomeOverride nomeOverride) {
		this.nomeOverride = nomeOverride;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AlimentoPasto that = (AlimentoPasto) o;
		return id != null && Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
