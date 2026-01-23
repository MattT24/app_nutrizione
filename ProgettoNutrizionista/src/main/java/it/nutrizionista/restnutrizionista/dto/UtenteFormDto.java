package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Form create/update per Utente (id nullo = create). */
public class UtenteFormDto {
    private Long id;

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 16, message = "Il nome non può superare i 16 caratteri")
    // Regex: Accetta lettere (incluse accentate), spazi e apostrofi. Niente numeri o simboli strani.
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u00FF\\s']+$", message = "Il nome può contenere solo lettere, spazi o apostrofi")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(max = 16, message = "Il cognome non può superare i 16 caratteri")
    // Regex: Idem come sopra
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u00FF\\s']+$", message = "Il cognome può contenere solo lettere, spazi o apostrofi")
    private String cognome;

    @NotBlank(message = "Il codice fiscale è obbligatorio")
    @Size(min = 16, max = 16, message = "Il codice fiscale deve essere di 16 caratteri")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Il codice fiscale contiene caratteri non validi")
    private String codiceFiscale;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "L'email non è valida")
    private String email;

    // VALIDAZIONE PASSWORD COMPLESSA
    // ^                 : Inizio stringa
    // (?=.*[A-Z])       : Lookahead: deve esserci almeno una maiuscola
    // (?=.*\\d)         : Lookahead: deve esserci almeno un numero
    // .{8,}             : Qualsiasi carattere, minimo 8 volte
    // $                 : Fine stringa
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$", 
             message = "La password deve avere almeno 8 caratteri, una maiuscola e un numero")
    private String password;

    @Past(message = "La data di nascita deve essere nel passato")
    private LocalDate dataNascita;

    @Pattern(regexp = "^\\d+$", message = "Il telefono deve contenere solo numeri")
    private String telefono;
    
    @Size(max = 50, message = "L'indirizzo è troppo lungo (massimo 50 caratteri)")
    @Pattern(regexp = "^[a-zA-Z0-9\\u00C0-\\u00FF\\s,.'\\-/#]+$", 
             message = "L'indirizzo contiene caratteri non validi (sono permessi solo lettere, numeri, spazi e punteggiatura standard)")
    private String indirizzo;
    
    @NotNull(message = "Il ruolo è obbligatorio")
    private RuoloDto ruolo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDate getDataNascita() { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
	public RuoloDto getRuolo() {return ruolo;}
	public void setRuolo(RuoloDto ruolo) {this.ruolo = ruolo;}
}
