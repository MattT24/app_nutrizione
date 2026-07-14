package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body della registrazione: campi minimi, ruolo assegnato automaticamente a USER. */
public class RegisterRequest {
    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    private String cognome;

    @NotBlank(message = "Il codice fiscale è obbligatorio")
    private String codiceFiscale;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "L'email non è valida")
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    private String password;

    private String telefono;

    @NotBlank(message = "L'indirizzo è obbligatorio")
    private String indirizzo;

    // A4/A9: accettazione documenti alla registrazione (presa visione/accettazione, non "consenso").
    @AssertTrue(message = "Devi prendere visione dell'Informativa Privacy")
    private boolean accettaPrivacy;

    @AssertTrue(message = "Devi accettare i Termini di Servizio")
    private boolean accettaTermini;

    @AssertTrue(message = "Devi accettare il DPA (accordo sul trattamento dei dati)")
    private boolean accettaDpa;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    public String getCodiceFiscale() { return codiceFiscale; }
    public void setCodiceFiscale(String codiceFiscale) { this.codiceFiscale = codiceFiscale; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    public boolean isAccettaPrivacy() { return accettaPrivacy; }
    public void setAccettaPrivacy(boolean accettaPrivacy) { this.accettaPrivacy = accettaPrivacy; }
    public boolean isAccettaTermini() { return accettaTermini; }
    public void setAccettaTermini(boolean accettaTermini) { this.accettaTermini = accettaTermini; }
    public boolean isAccettaDpa() { return accettaDpa; }
    public void setAccettaDpa(boolean accettaDpa) { this.accettaDpa = accettaDpa; }
}