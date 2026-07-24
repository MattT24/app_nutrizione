package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request per l'onboarding admin-invite di un nutrizionista (beta Keycloak). Minimale: solo identità di base.
 * Il codice fiscale (placeholder alla creazione) e telefono/indirizzo vengono completati dal nutrizionista al 1° accesso.
 * La password NON è qui: arriva via invito email Keycloak (UPDATE_PASSWORD).
 */
public class CreaNutrizionistaRequest {

    @NotBlank
    @Size(max = 16)
    private String nome;

    @NotBlank
    @Size(max = 16)
    private String cognome;

    @NotBlank
    @Email
    private String email;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
