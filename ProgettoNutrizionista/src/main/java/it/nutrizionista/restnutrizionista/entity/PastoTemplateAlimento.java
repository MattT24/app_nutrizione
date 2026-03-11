package it.nutrizionista.restnutrizionista.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "pasti_template_alimenti", uniqueConstraints = @UniqueConstraint(columnNames = { "template_id",
		"alimento_id" }))
public class PastoTemplateAlimento {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	private PastoTemplate template;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alimento_id", nullable = false)
	private AlimentoBase alimento;

	@Column(name = "quantita", nullable = false)
	private Double quantita;

	@Column(name = "nome_custom")
	private String nomeCustom;

	@OneToMany(mappedBy = "templateAlimento", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
	private Set<PastoTemplateAlternativo> alternative = new LinkedHashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PastoTemplate getTemplate() {
		return template;
	}

	public void setTemplate(PastoTemplate template) {
		this.template = template;
	}

	public AlimentoBase getAlimento() {
		return alimento;
	}

	public void setAlimento(AlimentoBase alimento) {
		this.alimento = alimento;
	}

	public Double getQuantita() {
		return quantita;
	}

	public void setQuantita(Double quantita) {
		this.quantita = quantita;
	}

	public String getNomeCustom() {
		return nomeCustom;
	}

	public void setNomeCustom(String nomeCustom) {
		this.nomeCustom = nomeCustom;
	}

	public Set<PastoTemplateAlternativo> getAlternative() {
		return alternative;
	}

	public void setAlternative(Set<PastoTemplateAlternativo> alternative) {
		this.alternative = alternative;
	}
}
