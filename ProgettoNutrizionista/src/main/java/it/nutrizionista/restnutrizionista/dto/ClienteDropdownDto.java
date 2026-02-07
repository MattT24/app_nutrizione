package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

public class ClienteDropdownDto {
    private Long id;
    private String nome;
    private String cognome;
    private LocalDate dataNascita;
    private String email;

    public ClienteDropdownDto() {}

    public ClienteDropdownDto(Long id, String nome, String cognome, LocalDate dataNascita, String email) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.dataNascita = dataNascita;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public LocalDate getDataNascita() { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
    
}

