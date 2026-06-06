package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Richiesta di tracciamento di un'attività su un cliente.
 */
public class AttivitaTrackRequest {

    @NotNull
    private Long clienteId;

    private String tipo;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
