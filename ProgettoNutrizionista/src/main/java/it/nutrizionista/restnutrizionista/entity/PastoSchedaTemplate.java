package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pasti_scheda_template")
@EntityListeners(AuditingEntityListener.class)
public class PastoSchedaTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	@Column(columnDefinition = "TEXT")
	private String descrizione;

	@Enumerated(EnumType.STRING)
	@Column(name = "giorno")
	private GiornoSettimana giorno;

	@Column(name = "ordine_visualizzazione", nullable = false)
	private Integer ordineVisualizzazione = 0;

	@Column(name = "orario_inizio")
	private LocalTime orarioInizio;

	@Column(name = "orario_fine")
	private LocalTime orarioFine;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheda_template_id", nullable = false)
	private SchedaTemplate schedaTemplate;

	@OneToMany(mappedBy = "pastoSchedaTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AlimentoPastoSchedaTemplate> alimenti = new ArrayList<>();

	@CreatedDate
	@Column(nullable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }
	public String getDescrizione() { return descrizione; }
	public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
	public GiornoSettimana getGiorno() { return giorno; }
	public void setGiorno(GiornoSettimana giorno) { this.giorno = giorno; }
	public Integer getOrdineVisualizzazione() { return ordineVisualizzazione; }
	public void setOrdineVisualizzazione(Integer ordineVisualizzazione) { this.ordineVisualizzazione = ordineVisualizzazione; }
	public LocalTime getOrarioInizio() { return orarioInizio; }
	public void setOrarioInizio(LocalTime orarioInizio) { this.orarioInizio = orarioInizio; }
	public LocalTime getOrarioFine() { return orarioFine; }
	public void setOrarioFine(LocalTime orarioFine) { this.orarioFine = orarioFine; }
	public SchedaTemplate getSchedaTemplate() { return schedaTemplate; }
	public void setSchedaTemplate(SchedaTemplate schedaTemplate) { this.schedaTemplate = schedaTemplate; }
	public List<AlimentoPastoSchedaTemplate> getAlimenti() { return alimenti; }
	public void setAlimenti(List<AlimentoPastoSchedaTemplate> alimenti) { this.alimenti = alimenti; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
