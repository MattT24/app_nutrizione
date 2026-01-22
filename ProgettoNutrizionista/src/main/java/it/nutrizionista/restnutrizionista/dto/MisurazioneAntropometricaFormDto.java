package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MisurazioneAntropometricaFormDto {

	private Long id;
	private Double spalle;
	private Double vita;
	private Double fianchi;
	private Double torace;
	private Double gambaS;
	private Double gambaD;
	private Double bicipiteS;
	private Double bicipiteD;
	private LocalDate dataMisurazione;
	@NotNull(message = "Il cliente è obbligatorio")
	private ClienteDto cliente;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Double getSpalle() {
		return spalle;
	}
	public void setSpalle(Double spalle) {
		this.spalle = spalle;
	}
	public Double getVita() {
		return vita;
	}
	public void setVita(Double vita) {
		this.vita = vita;
	}
	public Double getFianchi() {
		return fianchi;
	}
	public void setFianchi(Double fianchi) {
		this.fianchi = fianchi;
	}
	public Double getTorace() {
		return torace;
	}
	public void setTorace(Double torace) {
		this.torace = torace;
	}
	public Double getGambaS() {
		return gambaS;
	}
	public void setGambaS(Double gambaS) {
		this.gambaS = gambaS;
	}
	public Double getGambaD() {
		return gambaD;
	}
	public void setGambaD(Double gambaD) {
		this.gambaD = gambaD;
	}
	public Double getBicipiteS() {
		return bicipiteS;
	}
	public void setBicipiteS(Double bicipiteS) {
		this.bicipiteS = bicipiteS;
	}
	public Double getBicipiteD() {
		return bicipiteD;
	}
	public void setBicipiteD(Double bicipiteD) {
		this.bicipiteD = bicipiteD;
	}
	public LocalDate getDataMisurazione() {
		return dataMisurazione;
	}
	public void setDataMisurazione(LocalDate dataMisurazione) {
		this.dataMisurazione = dataMisurazione;
	}
	public ClienteDto getCliente() {
		return cliente;
	}
	public void setCliente(ClienteDto cliente) {
		this.cliente = cliente;
	}
	
	
}
