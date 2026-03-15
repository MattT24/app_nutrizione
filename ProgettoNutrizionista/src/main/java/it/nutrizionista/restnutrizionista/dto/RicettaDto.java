package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

public class RicettaDto {

	private Long id;
	private String titolo;
	private String descrizione;
	private String categoria;
	private String urlImmagine;
	private String fonte;
	private Boolean pubblica;
	private List<RicettaIngredienteDto> ingredienti;

	/** Macro totali calcolati server-side per porzione standard. */
	private MacroRicettaDto macroTotali;

	// ── Getters & Setters ──────────────────────────────────────────────────────

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getTitolo() { return titolo; }
	public void setTitolo(String titolo) { this.titolo = titolo; }

	public String getDescrizione() { return descrizione; }
	public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

	public String getCategoria() { return categoria; }
	public void setCategoria(String categoria) { this.categoria = categoria; }

	public String getUrlImmagine() { return urlImmagine; }
	public void setUrlImmagine(String urlImmagine) { this.urlImmagine = urlImmagine; }

	public String getFonte() { return fonte; }
	public void setFonte(String fonte) { this.fonte = fonte; }

	public Boolean getPubblica() { return pubblica; }
	public void setPubblica(Boolean pubblica) { this.pubblica = pubblica; }

	public List<RicettaIngredienteDto> getIngredienti() { return ingredienti; }
	public void setIngredienti(List<RicettaIngredienteDto> ingredienti) { this.ingredienti = ingredienti; }

	public MacroRicettaDto getMacroTotali() { return macroTotali; }
	public void setMacroTotali(MacroRicettaDto macroTotali) { this.macroTotali = macroTotali; }

	// ── Inner DTO for macro totals ─────────────────────────────────────────────

	public static class MacroRicettaDto {
		private double calorie;
		private double proteine;
		private double carboidrati;
		private double grassi;

		public MacroRicettaDto() {}
		public MacroRicettaDto(double calorie, double proteine, double carboidrati, double grassi) {
			this.calorie = calorie; this.proteine = proteine;
			this.carboidrati = carboidrati; this.grassi = grassi;
		}

		public double getCalorie() { return calorie; }
		public void setCalorie(double calorie) { this.calorie = calorie; }
		public double getProteine() { return proteine; }
		public void setProteine(double proteine) { this.proteine = proteine; }
		public double getCarboidrati() { return carboidrati; }
		public void setCarboidrati(double carboidrati) { this.carboidrati = carboidrati; }
		public double getGrassi() { return grassi; }
		public void setGrassi(double grassi) { this.grassi = grassi; }
	}
}
