package it.nutrizionista.restnutrizionista.dto;


public class MacroDto {

	 	private Long id;
	    private AlimentoBaseDto alimento;
		private Double calorie;
	    private Double grassi;
	    private Double proteine;
	    private Double carboidrati;

	    
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
		public Double getCalorie() {
			return calorie;
		}
		public void setCalorie(Double calorie) {
			this.calorie = calorie;
		}
		public Double getGrassi() {
			return grassi;
		}
		public void setGrassi(Double grassi) {
			this.grassi = grassi;
		}
		public Double getProteine() {
			return proteine;
		}
		public void setProteine(Double proteine) {
			this.proteine = proteine;
		}
		public Double getCarboidrati() {
			return carboidrati;
		}
		public void setCarboidrati(Double carboidrati) {
			this.carboidrati = carboidrati;
		}
	    
	    
}
