package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

/** DTO semplice per GET con nome nel body. */
public class NomeRequest{
	
    @NotNull(message = "Il nome è obbligatorio")
    private String nome;

    public NomeRequest() {}
    public NomeRequest(String nome) { this.nome = nome; }
	
    public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
    
    
}
