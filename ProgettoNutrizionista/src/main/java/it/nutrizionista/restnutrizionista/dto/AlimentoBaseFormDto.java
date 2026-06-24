package it.nutrizionista.restnutrizionista.dto;


import java.util.List;
import java.util.Map;
import java.util.Set;

import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.FonteAllergene;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AlimentoBaseFormDto {

    private Long id;
    
    @NotBlank(message = "Il nome è obbligatorio")
	private String nome;

    @NotNull(message = "I macro sono obbligatori")
    @Valid
    private MacroDto macroNutrienti;
    
    @NotNull(message = "La misura è obbligatoria")
    private Double misuraInGrammi;

    private List<ValoreMicroFormDto> microNutrienti;

	private String categoria;
    private String urlImmagine;
    private Set<String> tracce;

    // ── Tag dietetici (D4) ──────────────────────────────────────────
    private Boolean senzaGlutine;
    private Boolean senzaLattosio;
    private Boolean vegano;

    // ── Integrazione OpenFoodFacts ──────────────────────────────────
    private String barcode;
    private Map<Allergene, StatoAllergene> allergeni;
    private FonteAllergene fonteAllergeni;
    private String nutriscoreGrade;
    private Integer novaGroup;
    private String environmentalScoreGrade;
    private Map<String, String> nutrientLevels;
    private Set<String> additivi;
    private String ingredientsText;
    private Double servingQuantityG;
    private Boolean needsReview;

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

    // ── Tag booleani (D4) ────────────────────────────────────────────
    public Boolean getSenzaGlutine() { return senzaGlutine; }
    public void setSenzaGlutine(Boolean senzaGlutine) { this.senzaGlutine = senzaGlutine; }
    public Boolean getSenzaLattosio() { return senzaLattosio; }
    public void setSenzaLattosio(Boolean senzaLattosio) { this.senzaLattosio = senzaLattosio; }
    public Boolean getVegano() { return vegano; }
    public void setVegano(Boolean vegano) { this.vegano = vegano; }

    // ── Getters/Setters integrazione OFF ──
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public Map<Allergene, StatoAllergene> getAllergeni() { return allergeni; }
    public void setAllergeni(Map<Allergene, StatoAllergene> allergeni) { this.allergeni = allergeni; }
    public FonteAllergene getFonteAllergeni() { return fonteAllergeni; }
    public void setFonteAllergeni(FonteAllergene fonteAllergeni) { this.fonteAllergeni = fonteAllergeni; }
    public String getNutriscoreGrade() { return nutriscoreGrade; }
    public void setNutriscoreGrade(String nutriscoreGrade) { this.nutriscoreGrade = nutriscoreGrade; }
    public Integer getNovaGroup() { return novaGroup; }
    public void setNovaGroup(Integer novaGroup) { this.novaGroup = novaGroup; }
    public String getEnvironmentalScoreGrade() { return environmentalScoreGrade; }
    public void setEnvironmentalScoreGrade(String environmentalScoreGrade) { this.environmentalScoreGrade = environmentalScoreGrade; }
    public Map<String, String> getNutrientLevels() { return nutrientLevels; }
    public void setNutrientLevels(Map<String, String> nutrientLevels) { this.nutrientLevels = nutrientLevels; }
    public Set<String> getAdditivi() { return additivi; }
    public void setAdditivi(Set<String> additivi) { this.additivi = additivi; }
    public String getIngredientsText() { return ingredientsText; }
    public void setIngredientsText(String ingredientsText) { this.ingredientsText = ingredientsText; }
    public Double getServingQuantityG() { return servingQuantityG; }
    public void setServingQuantityG(Double servingQuantityG) { this.servingQuantityG = servingQuantityG; }
    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean needsReview) { this.needsReview = needsReview; }
}

