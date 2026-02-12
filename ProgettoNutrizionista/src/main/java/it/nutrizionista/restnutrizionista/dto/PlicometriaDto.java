package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

import it.nutrizionista.restnutrizionista.entity.Metodo;
import jakarta.persistence.Column;

public class PlicometriaDto {

    private Long id;
	
	@Column(name = "data_misurazione")
	private LocalDate dataMisurazione;
	private ClienteDto cliente;
	private Double tricipite;
    private Double bicipite;      // Nuovo (usato in Durnin)
    private Double sottoscapolare;
    private Double sovrailiaca;
    private Double addominale;
    private Double coscia;
    private Double pettorale;     // Nuovo (usato in JP3 Uomo)
    private Double ascellare;     // Nuovo (usato in JP7)
    private Double polpaccio;     // Nuovo (usato in altri metodi)
    private Double percentualeMassaGrassa;

    private Double pesoKgRiferimento;
    private Double sommaPliche;
    private Double densitaCorporea;
    private Double massaGrassaKg;
    private Double massaMagraKg;
    private String note;
    private Metodo metodo;
	
	public Long getId() {
		return id;
	}
	public Double getTricipite() {
		return tricipite;
	}
	public void setTricipite(Double tricipite) {
		this.tricipite = tricipite;
	}
	public Double getBicipite() {
		return bicipite;
	}
	public void setBicipite(Double bicipite) {
		this.bicipite = bicipite;
	}
	public Double getSottoscapolare() {
		return sottoscapolare;
	}
	public void setSottoscapolare(Double sottoscapolare) {
		this.sottoscapolare = sottoscapolare;
	}
	public Double getSovrailiaca() {
		return sovrailiaca;
	}
	public void setSovrailiaca(Double sovrailiaca) {
		this.sovrailiaca = sovrailiaca;
	}
	public Double getAddominale() {
		return addominale;
	}
	public void setAddominale(Double addominale) {
		this.addominale = addominale;
	}
	public Double getCoscia() {
		return coscia;
	}
	public void setCoscia(Double coscia) {
		this.coscia = coscia;
	}
	public Double getPettorale() {
		return pettorale;
	}
	public void setPettorale(Double pettorale) {
		this.pettorale = pettorale;
	}
	public Double getAscellare() {
		return ascellare;
	}
	public void setAscellare(Double ascellare) {
		this.ascellare = ascellare;
	}
	public Double getPolpaccio() {
		return polpaccio;
	}
	public void setPolpaccio(Double polpaccio) {
		this.polpaccio = polpaccio;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public LocalDate getDataMisurazione() {
		return dataMisurazione;
	}
	public void setDataMisurazione(LocalDate dataMisurazione) {
		this.dataMisurazione = dataMisurazione;
	}
	
	public Metodo getMetodo() {
		return metodo;
	}
	public void setMetodo(Metodo metodo) {
		this.metodo = metodo;
	}
	public ClienteDto getCliente() {
		return cliente;
	}
	public void setCliente(ClienteDto cliente) {
		this.cliente = cliente;
	}
	public Double getPesoKgRiferimento() {
		return pesoKgRiferimento;
	}
	public void setPesoKgRiferimento(Double pesoKgRiferimento) {
		this.pesoKgRiferimento = pesoKgRiferimento;
	}
	public Double getSommaPliche() {
		return sommaPliche;
	}
	public void setSommaPliche(Double sommaPliche) {
		this.sommaPliche = sommaPliche;
	}
	public Double getDensitaCorporea() {
		return densitaCorporea;
	}
	public void setDensitaCorporea(Double densitaCorporea) {
		this.densitaCorporea = densitaCorporea;
	}
	public Double getMassaGrassaKg() {
		return massaGrassaKg;
	}
	public void setMassaGrassaKg(Double massaGrassaKg) {
		this.massaGrassaKg = massaGrassaKg;
	}
	public Double getMassaMagraKg() {
		return massaMagraKg;
	}
	public void setMassaMagraKg(Double massaMagraKg) {
		this.massaMagraKg = massaMagraKg;
	}
	public Double getPercentualeMassaGrassa() {
		return percentualeMassaGrassa;
	}
	public void setPercentualeMassaGrassa(Double percentualeMassaGrassa) {
		this.percentualeMassaGrassa = percentualeMassaGrassa;
	}
	

	
	
	
}
