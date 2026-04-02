package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

public class CopyResultItemDto {

    private Long clienteId;
    private String clienteNomeCompleto;
    private Long nuovaSchedaId;
    private List<String> alimentiConflitto;
    private boolean stesso;

    public CopyResultItemDto() {}

    /** Factory per un risultato di successo */
    public static CopyResultItemDto successo(Long clienteId, String nomeCompleto, Long nuovaSchedaId, boolean stesso) {
        CopyResultItemDto item = new CopyResultItemDto();
        item.clienteId = clienteId;
        item.clienteNomeCompleto = nomeCompleto;
        item.nuovaSchedaId = nuovaSchedaId;
        item.stesso = stesso;
        return item;
    }

    /** Factory per un risultato con conflitto */
    public static CopyResultItemDto conflitto(Long clienteId, String nomeCompleto, List<String> alimentiConflitto) {
        CopyResultItemDto item = new CopyResultItemDto();
        item.clienteId = clienteId;
        item.clienteNomeCompleto = nomeCompleto;
        item.alimentiConflitto = alimentiConflitto;
        item.stesso = false;
        return item;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNomeCompleto() {
        return clienteNomeCompleto;
    }

    public void setClienteNomeCompleto(String clienteNomeCompleto) {
        this.clienteNomeCompleto = clienteNomeCompleto;
    }

    public Long getNuovaSchedaId() {
        return nuovaSchedaId;
    }

    public void setNuovaSchedaId(Long nuovaSchedaId) {
        this.nuovaSchedaId = nuovaSchedaId;
    }

    public List<String> getAlimentiConflitto() {
        return alimentiConflitto;
    }

    public void setAlimentiConflitto(List<String> alimentiConflitto) {
        this.alimentiConflitto = alimentiConflitto;
    }

    public boolean isStesso() {
        return stesso;
    }

    public void setStesso(boolean stesso) {
        this.stesso = stesso;
    }
}
