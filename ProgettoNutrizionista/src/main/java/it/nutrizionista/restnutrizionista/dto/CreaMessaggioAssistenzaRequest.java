package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Testo di un messaggio; il mittente deriva sempre dal JWT. */
public class CreaMessaggioAssistenzaRequest {
    @NotBlank @Size(max = 2000)
    private String testo;
    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }
}
