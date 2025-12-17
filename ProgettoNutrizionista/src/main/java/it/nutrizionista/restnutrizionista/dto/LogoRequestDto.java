package it.nutrizionista.restnutrizionista.dto;

import org.springframework.web.multipart.MultipartFile;


import jakarta.validation.constraints.NotNull;

public class LogoRequestDto {
	
    @NotNull(message = "L'id utente è obbligatorio")
    private Long utenteId;

    @NotNull(message = "Il file è obbligatorio")
    private MultipartFile image;


	public Long getUtenteId() {
		return utenteId;
	}

	public void setUtenteId(Long utenteId) {
		this.utenteId = utenteId;
	}

	public MultipartFile getImage() {
		return image;
	}

	public void setImage(MultipartFile image) {
		this.image = image;
	}
	
	
}
