package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

public class DisplayNameRequest {
	@NotBlank(message = "Il nome è obbligatorio")
	private String nome;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}
}

