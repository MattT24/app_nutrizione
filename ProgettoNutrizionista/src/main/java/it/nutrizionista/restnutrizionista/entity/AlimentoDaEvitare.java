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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "alimenti_da_evitare_per_cliente",
uniqueConstraints = {
        // VINCOLO FONDAMENTALE: Impedisce di inserire due volte "Mela" per lo stesso "Mario"
        @UniqueConstraint(columnNames = {"cliente_id", "alimento_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class AlimentoDaEvitare {
	
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
    @JoinColumn(name = "alimento_id")
	private AlimentoBase alimento;
	
	@ManyToOne
    @JoinColumn(name = "cliente_id")
	private Cliente cliente;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "tipo_restrizione")
    private TipoRestrizione tipo; 

    @Column(name = "note")
    private String note;
	
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
	public AlimentoBase getAlimento() {
		return alimento;
	}
	public void setAlimento(AlimentoBase alimento) {
		this.alimento = alimento;
	}
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
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
	public TipoRestrizione getTipo() {
		return tipo;
	}
	public void setTipo(TipoRestrizione tipo) {
		this.tipo = tipo;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
    
    

}
