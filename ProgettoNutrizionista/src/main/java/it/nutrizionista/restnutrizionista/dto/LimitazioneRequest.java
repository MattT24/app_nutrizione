package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Size;

/**
 * Corpo della richiesta di attivazione della limitazione del trattamento (A5.3, art. 18 GDPR).
 * L'id del cliente arriva dal path; qui viaggia solo la motivazione (facoltativa, registrata
 * nell'audit A7). La revoca non ha corpo.
 */
public class LimitazioneRequest {

    @Size(max = 512, message = "Il motivo non può superare i 512 caratteri")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
