package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.Formula;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "schede")
@EntityListeners(AuditingEntityListener.class)
public class Scheda {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
    @Column(nullable = false)
    private String nome;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
	private Cliente cliente;
	
    @Column(nullable = false)
    private Boolean attiva;
    
    @OneToMany(mappedBy = "scheda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordineVisualizzazione ASC, id ASC")
    private Set<Pasto> pasti = new LinkedHashSet<>();
    
	@Column(name = "data_creazione")
    private LocalDate dataCreazione;
    
	// Calcola il numero dei pasti direttamente in SQL senza caricare la lista!
    @Formula("(SELECT COUNT(*) FROM pasti p WHERE p.scheda_id = id)")
    private Integer numeroPasti;

    // ... aggiungi i getter e setter in fondo ...
    public Integer getNumeroPasti() {
        return numeroPasti;
    }

    public void setNumeroPasti(Integer numeroPasti) {
        this.numeroPasti = numeroPasti;
    }
	
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
	public Set<Pasto> getPasti() {
		return pasti;
	}
	public void setPasti(Set<Pasto> pasti) {
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
	public LocalDate getDataCreazione() {
		return dataCreazione;
	}
	public void setDataCreazione(LocalDate dataCreazione) {
		this.dataCreazione = dataCreazione;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
    
    
}
