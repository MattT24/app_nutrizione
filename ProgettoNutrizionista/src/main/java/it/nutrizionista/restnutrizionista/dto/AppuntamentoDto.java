package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import it.nutrizionista.restnutrizionista.entity.Appuntamento.Modalita;
import it.nutrizionista.restnutrizionista.entity.Appuntamento.StatoAppuntamento;

public class AppuntamentoDto {

    private Long id;
    private Long nutrizionistaId;
    private String nutrizionistaNome;
    private String nutrizionistaCognome;
    
    // Informazioni cliente (può essere registrato o non registrato)
    private Long clienteId; // Null se non registrato
    private String clienteNome;
    private String clienteCognome;
    private boolean clienteRegistrato; // Flag per sapere se è registrato
    
    private String descrizioneAppuntamento;
    private LocalDate data;
    private LocalTime ora;

    // ✅ Fine (persistita)
    private LocalDate endData;
    private LocalTime endOra;

    // ✅ timezone / all-day
    private String timezone;
    private boolean allDay;

    private Modalita modalita;
    private StatoAppuntamento stato;
    private String luogo;
    private String emailCliente;
    private Instant createdAt;
    private Instant updatedAt;

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNutrizionistaId() {
        return nutrizionistaId;
    }

    public void setNutrizionistaId(Long nutrizionistaId) {
        this.nutrizionistaId = nutrizionistaId;
    }

    public String getNutrizionistaNome() {
        return nutrizionistaNome;
    }

    public void setNutrizionistaNome(String nutrizionistaNome) {
        this.nutrizionistaNome = nutrizionistaNome;
    }

    public String getNutrizionistaCognome() {
        return nutrizionistaCognome;
    }

    public void setNutrizionistaCognome(String nutrizionistaCognome) {
        this.nutrizionistaCognome = nutrizionistaCognome;
    }

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


    public boolean isClienteRegistrato() {
        return clienteRegistrato;
    }

    public void setClienteRegistrato(boolean clienteRegistrato) {
        this.clienteRegistrato = clienteRegistrato;
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

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}