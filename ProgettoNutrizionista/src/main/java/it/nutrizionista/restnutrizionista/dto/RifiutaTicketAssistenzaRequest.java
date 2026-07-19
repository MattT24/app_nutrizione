package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Size;

/** Motivo facoltativo comunicato al nutrizionista in caso di rifiuto. */
public class RifiutaTicketAssistenzaRequest {
    @Size(max = 500)
    private String motivo;
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
