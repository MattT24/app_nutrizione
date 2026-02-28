package it.nutrizionista.restnutrizionista.dto;

public class PresetObiettivoDto {

	private Long id;
	private String nome;
	private Double pctProteine;
	private Double pctCarboidrati;
	private Double pctGrassi;
	private Double moltiplicatoreTdee;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

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
