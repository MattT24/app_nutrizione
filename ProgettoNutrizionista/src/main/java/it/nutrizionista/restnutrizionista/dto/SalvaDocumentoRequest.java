package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import jakarta.validation.constraints.NotNull;

public class SalvaDocumentoRequest {

    @NotNull
    private Long clienteId;

    @NotNull
    private TipoDocumento tipoDocumento;

    @NotNull
    private Long riferimentoId;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Long getRiferimentoId() {
        return riferimentoId;
    }

    public void setRiferimentoId(Long riferimentoId) {
        this.riferimentoId = riferimentoId;
    }
}
