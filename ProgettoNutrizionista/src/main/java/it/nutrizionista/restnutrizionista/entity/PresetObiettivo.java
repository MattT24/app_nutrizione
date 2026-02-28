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

@Entity
@Table(name = "preset_obiettivo")
public class PresetObiettivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "nutrizionista_id", nullable = false)
	private Utente nutrizionista;

	@Column(nullable = false)
	private String nome;

	@Column(name = "pct_proteine", nullable = false)
	private Double pctProteine;

	@Column(name = "pct_carboidrati", nullable = false)
	private Double pctCarboidrati;

	@Column(name = "pct_grassi", nullable = false)
	private Double pctGrassi;

	@Column(name = "moltiplicatore_tdee", nullable = false)
	private Double moltiplicatoreTdee = 1.0;

	// Getters & Setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Utente getNutrizionista() { return nutrizionista; }
	public void setNutrizionista(Utente nutrizionista) { this.nutrizionista = nutrizionista; }

	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }

	public Double getPctProteine() { return pctProteine; }
	public void setPctProteine(Double v) { this.pctProteine = v; }

	public Double getPctCarboidrati() { return pctCarboidrati; }
	public void setPctCarboidrati(Double v) { this.pctCarboidrati = v; }

	public Double getPctGrassi() { return pctGrassi; }
	public void setPctGrassi(Double v) { this.pctGrassi = v; }

	public Double getMoltiplicatoreTdee() { return moltiplicatoreTdee; }
	public void setMoltiplicatoreTdee(Double v) { this.moltiplicatoreTdee = v; }
}
