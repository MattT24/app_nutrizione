package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.entity.Appuntamento.Modalita;
import it.nutrizionista.restnutrizionista.entity.Appuntamento.StatoAppuntamento;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppuntamentoFormDto {

    private Long nutrizionistaId;
    private Long clienteId;
    private String clienteNome;
    private String clienteCognome;
    private boolean clienteRegistrato;
    private String emailCliente;
    
    private String descrizioneAppuntamento;
    
    private LocalDate data;
    private LocalTime ora;
    private LocalDate endData;
    private LocalTime endOra;
    
    private String timezone;
    private boolean allDay;
    
    private Modalita modalita;
    private StatoAppuntamento stato;
    private String luogo;

    public AppuntamentoFormDto() {
    }

    // Getters and Setters
    public Long getNutrizionistaId() { return nutrizionistaId; }
    public void setNutrizionistaId(Long nutrizionistaId) { this.nutrizionistaId = nutrizionistaId; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public String getClienteCognome() { return clienteCognome; }
    public void setClienteCognome(String clienteCognome) { this.clienteCognome = clienteCognome; }

    public boolean isClienteRegistrato() { return clienteRegistrato; }
    public void setClienteRegistrato(boolean clienteRegistrato) { this.clienteRegistrato = clienteRegistrato; }

    public String getEmailCliente() { return emailCliente; }
    public void setEmailCliente(String emailCliente) { this.emailCliente = emailCliente; }

    public String getDescrizioneAppuntamento() { return descrizioneAppuntamento; }
    public void setDescrizioneAppuntamento(String descrizioneAppuntamento) { this.descrizioneAppuntamento = descrizioneAppuntamento; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalTime getOra() { return ora; }
    public void setOra(LocalTime ora) { this.ora = ora; }

    public LocalDate getEndData() { return endData; }
    public void setEndData(LocalDate endData) { this.endData = endData; }

    public LocalTime getEndOra() { return endOra; }
    public void setEndOra(LocalTime endOra) { this.endOra = endOra; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }

    public Modalita getModalita() { return modalita; }
    public void setModalita(Modalita modalita) { this.modalita = modalita; }

    public StatoAppuntamento getStato() { return stato; }
    public void setStato(StatoAppuntamento stato) { this.stato = stato; }

    public String getLuogo() { return luogo; }
    public void setLuogo(String luogo) { this.luogo = luogo; }
}