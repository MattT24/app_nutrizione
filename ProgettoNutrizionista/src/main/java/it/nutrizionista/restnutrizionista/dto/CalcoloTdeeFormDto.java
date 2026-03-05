package it.nutrizionista.restnutrizionista.dto;

public class CalcoloTdeeFormDto {
    private Long clienteId;
    private String sesso;
    private Integer eta;
    private Double peso;
    private Double altezza;
    private Double livelloAttivita;

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

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
}