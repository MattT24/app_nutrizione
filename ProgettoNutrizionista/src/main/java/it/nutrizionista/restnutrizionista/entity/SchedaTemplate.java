package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "schede_template")
@EntityListeners(AuditingEntityListener.class)
public class SchedaTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	@Column(columnDefinition = "TEXT")
	private String descrizione;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoScheda tipo = TipoScheda.GIORNALIERA;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private Utente createdBy;

	@OneToMany(mappedBy = "schedaTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordineVisualizzazione ASC, id ASC")
	private List<PastoSchedaTemplate> pasti = new ArrayList<>();

	@CreatedDate
	@Column(nullable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }
	public String getDescrizione() { return descrizione; }
	public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
	public TipoScheda getTipo() { return tipo; }
	public void setTipo(TipoScheda tipo) { this.tipo = tipo; }
	public Utente getCreatedBy() { return createdBy; }
	public void setCreatedBy(Utente createdBy) { this.createdBy = createdBy; }
	public List<PastoSchedaTemplate> getPasti() { return pasti; }
	public void setPasti(List<PastoSchedaTemplate> pasti) { this.pasti = pasti; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
