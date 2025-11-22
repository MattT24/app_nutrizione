package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "clienti")
@EntityListeners(AuditingEntityListener.class)
public class Cliente {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false) private String nome;
    @Column(nullable = false) private String cognome;
 
    @Column(name = "codice_fiscale", nullable = false, unique = true)
    private String codiceFiscale;
    
    @Column(nullable = false, name = "data_nascita") private LocalDate dataNascita;
    @Column(nullable = false) private double peso;
    @Column(nullable = false) private int altezza;
    @Column(nullable = false, name = "num_allenamenti_settimanali") private String numAllenamentiSett;
    @Column(nullable = false) private String intolleranze;
    @Column(nullable = false, name = "funzioni_intestinali") private String funzioniIntestinali;
    @Column(nullable = false, name = "problematiche_salutari") private String problematicheSalutari;
    @Column(nullable = false, name = "quantita_qualita_sonno") private String quantitaEQualitaDelSonno;
    @Column(nullable = false, name = "assunzione_farmaci") private String assunzioneFarmaci;
    @Column(nullable = false, name = "beve_alcon") private Boolean beveAlcol;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlimentoDaEvitare> alimentiDaEvitare;
    
    @OneToOne
    @JoinColumn(name = "misurazione_antropometrica_id")
    private MisurazioneAntropometrica misurazioni;
    
    @ManyToOne
    @JoinColumn(name = "utente_id")
    private Utente nutrizionista;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Scheda> schede;
    
    @CreatedDate
    @Column(nullable = false) 
    private Instant createdAt;
    @LastModifiedDate
    @Column(nullable = false) 
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
	public String getCognome() {
		return cognome;
	}
	public void setCognome(String cognome) {
		this.cognome = cognome;
	}
	public String getCodiceFiscale() {
		return codiceFiscale;
	}
	public void setCodiceFiscale(String codiceFiscale) {
		this.codiceFiscale = codiceFiscale;
	}
	
	public LocalDate getDataNascita() {
		return dataNascita;
	}
	public void setDataNascita(LocalDate dataNascita) {
		this.dataNascita = dataNascita;
	}
	public double getPeso() {
		return peso;
	}
	public void setPeso(double peso) {
		this.peso = peso;
	}
	public int getAltezza() {
		return altezza;
	}
	public void setAltezza(int altezza) {
		this.altezza = altezza;
	}
	public String getNumAllenamentiSett() {
		return numAllenamentiSett;
	}
	public void setNumAllenamentiSett(String numAllenamentiSett) {
		this.numAllenamentiSett = numAllenamentiSett;
	}
	public String getIntolleranze() {
		return intolleranze;
	}
	public void setIntolleranze(String intolleranze) {
		this.intolleranze = intolleranze;
	}
	public String getFunzioniIntestinali() {
		return funzioniIntestinali;
	}
	public void setFunzioniIntestinali(String funzioniIntestinali) {
		this.funzioniIntestinali = funzioniIntestinali;
	}
	public String getProblematicheSalutari() {
		return problematicheSalutari;
	}
	public void setProblematicheSalutari(String problematicheSalutari) {
		this.problematicheSalutari = problematicheSalutari;
	}
	public String getQuantitaEQualitaDelSonno() {
		return quantitaEQualitaDelSonno;
	}
	public void setQuantitaEQualitaDelSonno(String quantitaEQualitaDelSonno) {
		this.quantitaEQualitaDelSonno = quantitaEQualitaDelSonno;
	}
	public String getAssunzioneFarmaci() {
		return assunzioneFarmaci;
	}
	public void setAssunzioneFarmaci(String assunzioneFarmaci) {
		this.assunzioneFarmaci = assunzioneFarmaci;
	}
	public Boolean getBeveAlcol() {
		return beveAlcol;
	}
	public void setBeveAlcol(Boolean beveAlcol) {
		this.beveAlcol = beveAlcol;
	}
	public List<AlimentoDaEvitare> getAlimentiDaEvitare() {
		return alimentiDaEvitare;
	}
	public void setAlimentiDaEvitare(List<AlimentoDaEvitare> alimentiDaEvitare) {
		this.alimentiDaEvitare = alimentiDaEvitare;
	}
	
	public MisurazioneAntropometrica getMisurazioni() {
		return misurazioni;
	}
	public void setMisurazioni(MisurazioneAntropometrica misurazioni) {
		this.misurazioni = misurazioni;
	}
	public Utente getNutrizionista() {
		return nutrizionista;
	}
	public void setNutrizionista(Utente nutrizionista) {
		this.nutrizionista = nutrizionista;
	}
	public List<Scheda> getSchede() {
		return schede;
	}
	public void setSchede(List<Scheda> schede) {
		this.schede = schede;
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
