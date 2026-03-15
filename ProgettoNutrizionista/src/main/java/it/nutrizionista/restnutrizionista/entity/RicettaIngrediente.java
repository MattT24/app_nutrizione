package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ricette_ingredienti",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ricetta_id", "alimento_id"}))
public class RicettaIngrediente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ricetta_id", nullable = false)
	private Ricetta ricetta;

	/** FK reale verso alimenti_base — garantisce ingredienti sempre validi. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alimento_id", nullable = false)
	private AlimentoBase alimento;

	/** Grammi per porzione standard della ricetta. */
	@Column(nullable = false)
	private Double quantita;

	/** Nome visualizzato opzionale (sovrascrive il nome del catalogo). */
	@Column(name = "nome_custom")
	private String nomeCustom;

	// ── Getters & Setters ──────────────────────────────────────────────────────

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Ricetta getRicetta() { return ricetta; }
	public void setRicetta(Ricetta ricetta) { this.ricetta = ricetta; }

	public AlimentoBase getAlimento() { return alimento; }
	public void setAlimento(AlimentoBase alimento) { this.alimento = alimento; }

	public Double getQuantita() { return quantita; }
	public void setQuantita(Double quantita) { this.quantita = quantita; }

	public String getNomeCustom() { return nomeCustom; }
	public void setNomeCustom(String nomeCustom) { this.nomeCustom = nomeCustom; }
}
