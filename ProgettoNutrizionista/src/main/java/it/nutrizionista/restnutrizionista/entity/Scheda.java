package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
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
@Table(name = "schede")
@EntityListeners(AuditingEntityListener.class)
public class Scheda {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
    @JoinColumn(name = "cliente_id")
	private Cliente cliente;
	
    @Column(nullable = false)
    private Boolean attiva;
    
    @OneToMany(mappedBy = "scheda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Pasto> pasti;
    
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
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	public Boolean getAttiva() {
		return attiva;
	}
	public void setAttiva(Boolean attiva) {
		this.attiva = attiva;
	}
	public List<Pasto> getPasti() {
		return pasti;
	}
	public void setPasti(List<Pasto> pasti) {
		this.pasti = pasti;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
    
    
}
