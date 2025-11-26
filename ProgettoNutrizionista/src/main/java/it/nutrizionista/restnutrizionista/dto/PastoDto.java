package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.NomePasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

public class PastoDto {

	    private Long id;
	    private NomePasto nome;
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
		public NomePasto getNome() {
			return nome;
		}
		public void setNome(NomePasto nome) {
			this.nome = nome;
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
