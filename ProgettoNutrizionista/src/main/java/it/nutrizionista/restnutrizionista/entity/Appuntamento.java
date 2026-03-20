package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appuntamenti")
@EntityListeners(AuditingEntityListener.class)
public class Appuntamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nutrizionista_id")
    private Utente nutrizionista;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private String clienteNome;
    private String clienteCognome;
    private boolean clienteRegistrato;

    @Column(length = 1000)
    private String descrizioneAppuntamento;

    @Column(nullable = false)
    private LocalDate data;
    
    private LocalTime ora;

    @Column(name = "end_data", nullable = false)
    private LocalDate endData;
    
    @Column(name = "end_ora")
    private LocalTime endOra;

    private String timezone;
    private boolean allDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalita modalita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoAppuntamento stato;

    private String luogo;
    private String emailCliente;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Modalita {
        IN_STUDIO, ONLINE, DOMICILIO
    }

    public enum StatoAppuntamento {
        PRENOTATO, COMPLETATO, ANNULLATO, NON_PRESENTATO
    }

    public Appuntamento() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utente getNutrizionista() { return nutrizionista; }
    public void setNutrizionista(Utente nutrizionista) { this.nutrizionista = nutrizionista; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public String getClienteCognome() { return clienteCognome; }
    public void setClienteCognome(String clienteCognome) { this.clienteCognome = clienteCognome; }

    public boolean isClienteRegistrato() { return clienteRegistrato; }
    public void setClienteRegistrato(boolean clienteRegistrato) { this.clienteRegistrato = clienteRegistrato; }

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

    public String getEmailCliente() { return emailCliente; }
    public void setEmailCliente(String emailCliente) { this.emailCliente = emailCliente; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}