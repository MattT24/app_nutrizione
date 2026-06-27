package it.nutrizionista.restnutrizionista.dto;

/**
 * Risposta di /api/auth/google/login.
 * Se registrationRequired = false, loginResponse contiene il JWT (utente già esistente).
 * Se registrationRequired = true, loginResponse è null: il frontend deve raccogliere
 * i campi mancanti (codiceFiscale, telefono, indirizzo) e chiamare /api/auth/google/register,
 * usando email/nome/cognome qui restituiti per precompilare il form.
 */
public class GoogleAuthResponse {

    private boolean registrationRequired;
    private String email;
    private String nome;
    private String cognome;
    private LoginResponse loginResponse;

    public boolean isRegistrationRequired() { return registrationRequired; }
    public void setRegistrationRequired(boolean registrationRequired) { this.registrationRequired = registrationRequired; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    public LoginResponse getLoginResponse() { return loginResponse; }
    public void setLoginResponse(LoginResponse loginResponse) { this.loginResponse = loginResponse; }
}
