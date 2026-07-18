package it.nutrizionista.restnutrizionista.dto;

/** Vista minima del nutrizionista esposta esclusivamente al super admin. */
public class NutrizionistaAssistenzaDto {
    private Long id;
    private String nome;
    private String cognome;
    private String email;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
