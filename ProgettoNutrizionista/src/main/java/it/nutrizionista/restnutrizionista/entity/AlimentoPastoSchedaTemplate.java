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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "alimenti_pasto_scheda_template")
@EntityListeners(AuditingEntityListener.class)
public class AlimentoPastoSchedaTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alimento_id", nullable = false)
	private AlimentoBase alimento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pasto_scheda_template_id", nullable = false)
	private PastoSchedaTemplate pastoSchedaTemplate;

	@Column(nullable = false)
	private int quantita;

	@Column(name = "nome_custom")
	private String nomeCustom;

	@Column(name = "ordine", nullable = false, columnDefinition = "int default 0")
	private int ordine = 0;

	@OneToMany(mappedBy = "alimentoPastoSchedaTemplate",
			cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<AlimentoSchedaTemplateAlternativa> alternative = new ArrayList<>();

	@CreatedDate
	@Column(nullable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public AlimentoBase getAlimento() { return alimento; }
	public void setAlimento(AlimentoBase alimento) { this.alimento = alimento; }
	public PastoSchedaTemplate getPastoSchedaTemplate() { return pastoSchedaTemplate; }
	public void setPastoSchedaTemplate(PastoSchedaTemplate pastoSchedaTemplate) { this.pastoSchedaTemplate = pastoSchedaTemplate; }
	public int getQuantita() { return quantita; }
	public void setQuantita(int quantita) { this.quantita = quantita; }
	public String getNomeCustom() { return nomeCustom; }
	public void setNomeCustom(String nomeCustom) { this.nomeCustom = nomeCustom; }
	public int getOrdine() { return ordine; }
	public void setOrdine(int ordine) { this.ordine = ordine; }
	public List<AlimentoSchedaTemplateAlternativa> getAlternative() { return alternative; }
	public void setAlternative(List<AlimentoSchedaTemplateAlternativa> alternative) { this.alternative = alternative; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
