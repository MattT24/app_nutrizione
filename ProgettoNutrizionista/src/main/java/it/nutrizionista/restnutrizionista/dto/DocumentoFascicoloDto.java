package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import java.time.Instant;

public class DocumentoFascicoloDto {
    private Long id;
    private Long clienteId;
    private String titolo;
    private TipoDocumento tipoDocumento;
    private Instant dataCreazione;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Instant getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(Instant dataCreazione) {
        this.dataCreazione = dataCreazione;
    }
}
