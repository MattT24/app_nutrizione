package it.nutrizionista.restnutrizionista.dto;


import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;

public class AlimentoBaseFormDto {

    private Long id;
    
    @NotBlank(message = "Il nome è obbligatorio")
	private String nome;

    @NotBlank(message = "I macro sono obbligatori")
    private MacroDto macroNutrienti;
    
    @NotBlank(message = "La misura è obbligatoria")
    private Double misuraInGrammi;
    private List<ValoreMicroFormDto> microNutrienti;
	private String categoria;
    private String urlImmagine;
    private Set<String> tracce;
	

    
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
	public List<ValoreMicroFormDto> getMicroNutrienti() {
		return microNutrienti;
	}
	public void setMicroNutrienti(List<ValoreMicroFormDto> microNutrienti) {
		this.microNutrienti = microNutrienti;
	}
    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    public String getUrlImmagine() {
        return urlImmagine;
    }
    public void setUrlImmagine(String urlImmagine) {
        this.urlImmagine = urlImmagine;
    }
    public Set<String> getTracce() {
        return tracce;
    }
    public void setTracce(Set<String> tracce) {
        this.tracce = tracce;
    }
}

