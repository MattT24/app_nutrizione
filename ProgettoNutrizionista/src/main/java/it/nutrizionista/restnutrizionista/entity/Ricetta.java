package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;

@Entity
@Table(name = "ricette")
@EntityListeners(AuditingEntityListener.class)
public class Ricetta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String titolo;


	private String descrizione;

	private String categoria;

	@Column(name = "url_immagine")
	private String urlImmagine;

	/** URL sorgente (web, libro, ecc.) */
	private String fonte;

	/** Se true, visibile a tutti i nutrizionisti. Se false, solo Admin. */
	@Column(nullable = false)
	private Boolean pubblica = true;

	@OneToMany(mappedBy = "ricetta", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RicettaIngrediente> ingredienti = new ArrayList<>();

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

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

	public List<RicettaIngrediente> getIngredienti() { return ingredienti; }
	public void setIngredienti(List<RicettaIngrediente> ingredienti) { this.ingredienti = ingredienti; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
