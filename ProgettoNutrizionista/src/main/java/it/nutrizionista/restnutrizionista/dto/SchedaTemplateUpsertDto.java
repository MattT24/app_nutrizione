package it.nutrizionista.restnutrizionista.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SchedaTemplateUpsertDto {
	@NotBlank(message = "Il nome del template e' obbligatorio")
	private String nome;
	private String descrizione;
	@NotNull(message = "Il tipo e' obbligatorio (GIORNALIERA o SETTIMANALE)")
	private String tipo;
	@Valid
	private List<PastoSchedaTemplateUpsertDto> pasti = new ArrayList<>();

	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }
	public String getDescrizione() { return descrizione; }
	public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public List<PastoSchedaTemplateUpsertDto> getPasti() { return pasti; }
	public void setPasti(List<PastoSchedaTemplateUpsertDto> pasti) { this.pasti = pasti; }
}
