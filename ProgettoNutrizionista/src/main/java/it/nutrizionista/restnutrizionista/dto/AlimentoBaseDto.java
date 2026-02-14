package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.util.List;

public class AlimentoBaseDto {
	
    private Long id;
	private String nome;
    private List<AlimentoPastoDto> alimentiScelti;
    private List<AlimentoDaEvitareDto> alimentiEvitati;
    private MacroDto macroNutrienti;
    private List<ValoreMicroDto> micronutrienti;
    private Double misuraInGrammi;
	private String categoria;
    private Instant createdAt;
    private Instant updatedAt;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public List<AlimentoPastoDto> getAlimentiScelti() {
		return alimentiScelti;
	}
	public void setAlimentiScelti(List<AlimentoPastoDto> alimentiScelti) {
		this.alimentiScelti = alimentiScelti;
	}
	public List<AlimentoDaEvitareDto> getAlimentiEvitati() {
		return alimentiEvitati;
	}
	public void setAlimentiEvitati(List<AlimentoDaEvitareDto> alimentiEvitati) {
		this.alimentiEvitati = alimentiEvitati;
	}
	public MacroDto getMacroNutrienti() {
		return macroNutrienti;
	}
	public void setMacroNutrienti(MacroDto macroNutrienti) {
		this.macroNutrienti = macroNutrienti;
	}
	public Double getMisuraInGrammi() {
		return misuraInGrammi;
	}
	public void setMisuraInGrammi(Double misuraInGrammi) {
		this.misuraInGrammi = misuraInGrammi;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
	public Instant getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
	public List<ValoreMicroDto> getMicronutrienti() {
		return micronutrienti;
	}
	public void setMicronutrienti(List<ValoreMicroDto> micronutrienti) {
		this.micronutrienti = micronutrienti;
	}

    
    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
