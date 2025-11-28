package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

import it.nutrizionista.restnutrizionista.entity.Sesso;
import jakarta.validation.constraints.NotBlank;

public class ClienteFormDto {

	private Long id;

    @NotBlank(message = "Il sesso è obbligatorio")
	private Sesso sesso;
    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    private String cognome;
    private String telefono;
    private String codiceFiscale;
    private String email;
    private LocalDate dataNascita;
    private double peso;
    private int altezza;
    private String numAllenamentiSett;
    private String intolleranze;
    private String funzioniIntestinali;
    private String problematicheSalutari;
    private String quantitaEQualitaDelSonno;
    private String assunzioneFarmaci;
    private Boolean beveAlcol;
    private MisurazioneAntropometricaDto misurazioni;
    private UtenteDto nutrizionista;
    
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
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
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
	public MisurazioneAntropometricaDto getMisurazioni() {
		return misurazioni;
	}
	public void setMisurazioni(MisurazioneAntropometricaDto misurazioni) {
		this.misurazioni = misurazioni;
	}
	public UtenteDto getNutrizionista() {
		return nutrizionista;
	}
	public void setNutrizionista(UtenteDto nutrizionista) {
		this.nutrizionista = nutrizionista;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

    
    
}
