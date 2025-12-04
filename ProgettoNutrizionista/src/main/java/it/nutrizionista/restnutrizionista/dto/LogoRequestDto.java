package it.nutrizionista.restnutrizionista.dto;

import org.springframework.web.multipart.MultipartFile;


import jakarta.validation.constraints.NotNull;

public class LogoRequestDto {
	
	@NotNull(message = "L'utente è obbligatorio")
	private UtenteDto utente;
	
	@NotNull(message = "Il file è obbligatorio")
	private MultipartFile image;

	public UtenteDto getUtente() {
		return utente;
	}

	public void setUtente(UtenteDto utente) {
		this.utente = utente;
	}

	public MultipartFile getImage() {
		return image;
	}

	public void setImage(MultipartFile image) {
		this.image = image;
	}
	
	
}
