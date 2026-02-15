package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;


public class PastoDto {

	    private Long id;
	    private String nome;
	    private String defaultCode;
	    private String descrizione;
	    private Integer ordineVisualizzazione;
	    private Boolean eliminabile;
	    private SchedaDto scheda;
	    private List<AlimentoPastoDto> alimentiPasto;
	    private LocalTime orarioInizio;
	    private LocalTime orarioFine;
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
		public String getDefaultCode() {
			return defaultCode;
		}
		public void setDefaultCode(String defaultCode) {
			this.defaultCode = defaultCode;
		}
		public String getDescrizione() {
			return descrizione;
		}
		public void setDescrizione(String descrizione) {
			this.descrizione = descrizione;
		}
		public Integer getOrdineVisualizzazione() {
			return ordineVisualizzazione;
		}
		public void setOrdineVisualizzazione(Integer ordineVisualizzazione) {
			this.ordineVisualizzazione = ordineVisualizzazione;
		}
		public Boolean getEliminabile() {
			return eliminabile;
		}
		public void setEliminabile(Boolean eliminabile) {
			this.eliminabile = eliminabile;
		}
		public SchedaDto getScheda() {
			return scheda;
		}
		public void setScheda(SchedaDto scheda) {
			this.scheda = scheda;
		}
		public List<AlimentoPastoDto> getAlimentiPasto() {
			return alimentiPasto;
		}
		public void setAlimentiPasto(List<AlimentoPastoDto> alimentiPasto) {
			this.alimentiPasto = alimentiPasto;
		}
		public LocalTime getOrarioInizio() {
			return orarioInizio;
		}
		public void setOrarioInizio(LocalTime orarioInizio) {
			this.orarioInizio = orarioInizio;
		}
		public LocalTime getOrarioFine() {
			return orarioFine;
		}
		public void setOrarioFine(LocalTime orarioFine) {
			this.orarioFine = orarioFine;
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
	    
}
