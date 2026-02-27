package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import it.nutrizionista.restnutrizionista.entity.Appuntamento.Modalita;
import it.nutrizionista.restnutrizionista.entity.Appuntamento.StatoAppuntamento;

public class AppuntamentoFormDto {

    // Se clienteId è null, allora i campi clienteNome, clienteCognome e emailCliente sono obbligatori
    private Long clienteId;
    
    // Campi per cliente non registrato (obbligatori se clienteId è null)
    private String clienteNome;
    private String clienteCognome;
    
    
    private String descrizioneAppuntamento;
    private LocalDate data;
    private LocalTime ora;

    // ✅ Fine (opzionale in input: se null -> default 60 min)
    private LocalDate endData;
    private LocalTime endOra;

    // ✅ timezone/allDay (opzionali in input)
    private String timezone;
    private Boolean allDay;

    private Modalita modalita;
    private StatoAppuntamento stato;
    private String luogo;
    private String emailCliente;

    
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
    }

 

    public String getDescrizioneAppuntamento() {
        return descrizioneAppuntamento;
    }

    public void setDescrizioneAppuntamento(String descrizioneAppuntamento) {
        this.descrizioneAppuntamento = descrizioneAppuntamento;
    }

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

    public LocalDate getEndData() {
        return endData;
    }

    public void setEndData(LocalDate endData) {
        this.endData = endData;
    }

    public LocalTime getEndOra() {
        return endOra;
    }

    public void setEndOra(LocalTime endOra) {
        this.endOra = endOra;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getAllDay() {
        return allDay;
    }

    public void setAllDay(Boolean allDay) {
        this.allDay = allDay;
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