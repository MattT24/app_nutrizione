package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;

public class ResetPasswordDto {

	@NotBlank(message = "La password è obbligatoria in creazione")
    private String password;
	
	@NotBlank(message = "Riconfermare la password è obbligatorio")
	private String confermaPassword;
	
	@NotBlank(message = "L'email è obbligatoria")
    @Email(message = "L'email non è valida")
    private String email;
	
	public String getPassword() {return password;}
	public void setPassword(String password) {this.password = password;}
	public String getConfermaPassword() {return confermaPassword;}
	public void setConfermaPassword(String confermaPassword) {this.confermaPassword = confermaPassword;}
	public String getEmail() {return email;}
	public void setEmail(String email) {this.email = email;}
	
}

