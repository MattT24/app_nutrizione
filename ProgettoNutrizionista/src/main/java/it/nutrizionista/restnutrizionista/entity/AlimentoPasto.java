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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "alimenti_pasto")
@EntityListeners(AuditingEntityListener.class)
public class AlimentoPasto {
 
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne
    @JoinColumn(name = "alimento_id")
    private AlimentoBase alimento;
	
    @ManyToOne
    @JoinColumn(name = "pasto_id")
    private Pasto pasto;
    
    @Column(nullable = false)
    private int quantita;
    
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
	
	
 
    
    
}
