package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "pasti_template_alternative",
	uniqueConstraints = @UniqueConstraint(columnNames = { "template_alimento_id", "alimento_alternativo_id" })
)
public class PastoTemplateAlternativo {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_alimento_id", nullable = false)
	private PastoTemplateAlimento templateAlimento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alimento_alternativo_id", nullable = false)
	private AlimentoBase alimentoAlternativo;

	@Column(name = "quantita", nullable = false)
	private Integer quantita = 100;

	@Column(name = "priorita", nullable = false)
	private Integer priorita = 1;

	@Column(name = "mode", nullable = false)
	@Enumerated(EnumType.STRING)
	private AlternativeMode mode = AlternativeMode.CALORIE;

	@Column(name = "manual", nullable = false)
	private Boolean manual = true;

	@Column(name = "note", length = 500)
	private String note;

	@Column(name = "nome_custom")
	private String nomeCustom;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PastoTemplateAlimento getTemplateAlimento() {
		return templateAlimento;
	}

	public void setTemplateAlimento(PastoTemplateAlimento templateAlimento) {
		this.templateAlimento = templateAlimento;
	}

	public AlimentoBase getAlimentoAlternativo() {
		return alimentoAlternativo;
	}

	public void setAlimentoAlternativo(AlimentoBase alimentoAlternativo) {
		this.alimentoAlternativo = alimentoAlternativo;
	}

	public Integer getQuantita() {
		return quantita;
	}

	public void setQuantita(Integer quantita) {
		this.quantita = quantita;
	}

	public Integer getPriorita() {
		return priorita;
	}

	public void setPriorita(Integer priorita) {
		this.priorita = priorita;
	}

	public AlternativeMode getMode() {
		return mode;
	}

	public void setMode(AlternativeMode mode) {
		this.mode = mode;
	}

	public Boolean getManual() {
		return manual;
	}

	public void setManual(Boolean manual) {
		this.manual = manual;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getNomeCustom() {
		return nomeCustom;
	}

	public void setNomeCustom(String nomeCustom) {
		this.nomeCustom = nomeCustom;
	}
}
