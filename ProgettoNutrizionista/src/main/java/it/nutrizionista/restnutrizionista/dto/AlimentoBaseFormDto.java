package it.nutrizionista.restnutrizionista.dto;


import jakarta.validation.constraints.NotBlank;

public class AlimentoBaseFormDto {

    private Long id;
    
    @NotBlank(message = "Il nome è obbligatorio")
	private String nome;

    @NotBlank(message = "I macro sono obbligatori")
    private MacroDto macroNutrienti;

    @NotBlank(message = "I micro sono obbligatorio")
    private MicroDto microNutrienti;
    
    @NotBlank(message = "La misura è obbligatoria")
    private Double misuraInGrammi;
 
    
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
	public MicroDto getMicroNutrienti() {
		return microNutrienti;
	}
	public void setMicroNutrienti(MicroDto microNutrienti) {
		this.microNutrienti = microNutrienti;
	}
	public Double getMisuraInGrammi() {
		return misuraInGrammi;
	}
	public void setMisuraInGrammi(Double misuraInGrammi) {
		this.misuraInGrammi = misuraInGrammi;
	}
    
    
}
