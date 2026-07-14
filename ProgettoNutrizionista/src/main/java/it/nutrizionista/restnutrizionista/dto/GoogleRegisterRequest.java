package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * Body per /api/auth/google/register: completa la registrazione di un nuovo
 * Utente avviata con Google. L'idToken viene riverificato lato server (non ci
 * si fida di nome/cognome/email passati dal client: arrivano dal token).
 * I campi qui richiesti sono quelli obbligatori su Utente che Google non fornisce.
 */
public class GoogleRegisterRequest {

    @NotBlank(message = "L'idToken è obbligatorio")
    private String idToken;

    @NotBlank(message = "Il codice fiscale è obbligatorio")
    private String codiceFiscale;

    @NotBlank(message = "Il telefono è obbligatorio")
    private String telefono;

    @NotBlank(message = "L'indirizzo è obbligatorio")
    private String indirizzo;

    @Past(message = "La data di nascita deve essere nel passato")
    private LocalDate dataNascita;

    // A4/A9: accettazione documenti anche per la registrazione via Google.
    @AssertTrue(message = "Devi prendere visione dell'Informativa Privacy")
    private boolean accettaPrivacy;

    @AssertTrue(message = "Devi accettare i Termini di Servizio")
    private boolean accettaTermini;

    @AssertTrue(message = "Devi accettare il DPA (accordo sul trattamento dei dati)")
    private boolean accettaDpa;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    public String getCodiceFiscale() { return codiceFiscale; }
    public void setCodiceFiscale(String codiceFiscale) { this.codiceFiscale = codiceFiscale; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    public LocalDate getDataNascita() { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }
    public boolean isAccettaPrivacy() { return accettaPrivacy; }
    public void setAccettaPrivacy(boolean accettaPrivacy) { this.accettaPrivacy = accettaPrivacy; }
    public boolean isAccettaTermini() { return accettaTermini; }
    public void setAccettaTermini(boolean accettaTermini) { this.accettaTermini = accettaTermini; }
    public boolean isAccettaDpa() { return accettaDpa; }
    public void setAccettaDpa(boolean accettaDpa) { this.accettaDpa = accettaDpa; }
}
