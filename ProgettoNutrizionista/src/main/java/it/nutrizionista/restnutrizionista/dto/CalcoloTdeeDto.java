package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

public class CalcoloTdeeDto {
    private Long id;
    private LocalDate dataCalcolo;
    private String sesso;
    private Integer eta;
    private Double peso;
    private Double altezza;
    private Double livelloAttivita;
    private Long bmr;
    private Long tdee;
    private Long clienteId;
    private Long tdeeSettimanale;
    private Long calorieDimagrimento;
    private Long calorieMassa;
    private Double fabbisognoIdrico;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDataCalcolo() { return dataCalcolo; }
    public void setDataCalcolo(LocalDate dataCalcolo) { this.dataCalcolo = dataCalcolo; }

    public String getSesso() { return sesso; }
    public void setSesso(String sesso) { this.sesso = sesso; }

    public Integer getEta() { return eta; }
    public void setEta(Integer eta) { this.eta = eta; }

    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }

    public Double getAltezza() { return altezza; }
    public void setAltezza(Double altezza) { this.altezza = altezza; }

    public Double getLivelloAttivita() { return livelloAttivita; }
    public void setLivelloAttivita(Double livelloAttivita) { this.livelloAttivita = livelloAttivita; }

    public Long getBmr() { return bmr; }
    public void setBmr(Long bmr) { this.bmr = bmr; }

    public Long getTdee() { return tdee; }
    public void setTdee(Long tdee) { this.tdee = tdee; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
	public Long getTdeeSettimanale() {
		return tdeeSettimanale;
	}
	public void setTdeeSettimanale(Long tdeeSettimanale) {
		this.tdeeSettimanale = tdeeSettimanale;
	}
	public Long getCalorieDimagrimento() {
		return calorieDimagrimento;
	}
	public void setCalorieDimagrimento(Long calorieDimagrimento) {
		this.calorieDimagrimento = calorieDimagrimento;
	}
	public Long getCalorieMassa() {
		return calorieMassa;
	}
	public void setCalorieMassa(Long calorieMassa) {
		this.calorieMassa = calorieMassa;
	}
	public Double getFabbisognoIdrico() {
		return fabbisognoIdrico;
	}
	public void setFabbisognoIdrico(Double fabbisognoIdrico) {
		this.fabbisognoIdrico = fabbisognoIdrico;
	}
    
    
}