package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import it.nutrizionista.restnutrizionista.entity.Appuntamento.Modalita;
import it.nutrizionista.restnutrizionista.entity.Appuntamento.StatoAppuntamento;

public class AppuntamentoFormDto {

    private Long clienteId;
    private String clienteNome;
    private String clienteCognome;
    private String descrizioneAppuntamento;
    private LocalDate data;
    private LocalTime ora;
    private Modalita modalita;
    private StatoAppuntamento stato;
    private String luogo;
    private String emailCliente;

    // Getter e Setter
    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getClienteCognome() {
        return clienteCognome;
    }

    public void setClienteCognome(String clienteCognome) {
        this.clienteCognome = clienteCognome;
    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getOra() {
        return ora;
    }

    public void setOra(LocalTime ora) {
        this.ora = ora;
    }

    public Modalita getModalita() {
        return modalita;
    }

    public void setModalita(Modalita modalita) {
        this.modalita = modalita;
    }

    public StatoAppuntamento getStato() {
        return stato;
    }

    public void setStato(StatoAppuntamento stato) {
        this.stato = stato;
    }

    public String getLuogo() {
        return luogo;
    }

    public void setLuogo(String luogo) {
        this.luogo = luogo;
    }

    public String getEmailCliente() {
        return emailCliente;
    }

    public void setEmailCliente(String emailCliente) {
        this.emailCliente = emailCliente;
    }
}