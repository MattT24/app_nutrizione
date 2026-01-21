package it.nutrizionista.restnutrizionista.dto;


import jakarta.validation.constraints.NotBlank;

public class ResetPasswordDto {

	@NotBlank(message = "La password è obbligatoria in creazione")
    private String password;
	
	@NotBlank(message = "Riconfermare la password è obbligatorio")
	private String confermaPassword;
	
	
	public String getPassword() {return password;}
	public void setPassword(String password) {this.password = password;}
	public String getConfermaPassword() {return confermaPassword;}
	public void setConfermaPassword(String confermaPassword) {this.confermaPassword = confermaPassword;}

	
}

