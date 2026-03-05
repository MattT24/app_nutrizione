package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "calcoli_tdee")
public class CalcoloTdee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataCalcolo;

    private String sesso;
    private Integer eta;
    private Double peso;
    private Double altezza;
    private Double livelloAttivita;

    @Column(nullable = false)
    private Double bmr;

    @Column(nullable = false)
    private Double tdee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    public CalcoloTdee() {}

    // Getters e Setters
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

    public Double getBmr() { return bmr; }
    public void setBmr(Double bmr) { this.bmr = bmr; }

    public Double getTdee() { return tdee; }
    public void setTdee(Double tdee) { this.tdee = tdee; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}