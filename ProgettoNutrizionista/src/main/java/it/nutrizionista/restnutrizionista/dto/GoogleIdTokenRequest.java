package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

/** Body per /api/auth/google/login: ID token firmato da Google Identity Services. */
public class GoogleIdTokenRequest {

    @NotBlank(message = "L'idToken è obbligatorio")
    private String idToken;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
}
