package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

public class MicroFormDto {

    private Long id;
    
    @NotNull(message = "Il nome è obbligatorio")
    private String nome;     // Ferro, Vitamina C, Zinco

    private String unita;    // mg, µg

    private String categoria; // vitamina, minerale, altro

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

	public String getUnita() {
		return unita;
	}

	public void setUnita(String unita) {
		this.unita = unita;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}
    
}
