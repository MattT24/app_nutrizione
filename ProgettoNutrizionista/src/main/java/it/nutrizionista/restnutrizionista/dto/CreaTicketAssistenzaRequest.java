package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dati consentiti per aprire un ticket; l'utente deriva sempre dal JWT. */
public class CreaTicketAssistenzaRequest {
    @NotBlank @Size(max = 120)
    private String oggetto;
    @NotBlank @Size(max = 2000)
    private String descrizione;
    public String getOggetto() { return oggetto; }
    public void setOggetto(String oggetto) { this.oggetto = oggetto; }
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
}
