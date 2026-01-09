package it.nutrizionista.restnutrizionista.dto;

public class ValoreMicroDto {
	
    private Long id;

    private AlimentoBaseDto alimento;

    private MicroDto micronutriente;

    private Double valore;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public AlimentoBaseDto getAlimento() {
		return alimento;
	}

	public void setAlimento(AlimentoBaseDto alimento) {
		this.alimento = alimento;
	}

	public MicroDto getMicronutriente() {
		return micronutriente;
	}

	public void setMicronutriente(MicroDto micronutriente) {
		this.micronutriente = micronutriente;
	}

	public Double getValore() {
		return valore;
	}

	public void setValore(Double valore) {
		this.valore = valore;
	}

}
