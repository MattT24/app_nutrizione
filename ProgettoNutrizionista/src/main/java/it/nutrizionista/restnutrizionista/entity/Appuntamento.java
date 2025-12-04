package it.nutrizionista.restnutrizionista.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
	    name = "appuntamenti",
	    uniqueConstraints = {
	        @jakarta.persistence.UniqueConstraint(
	            columnNames = {"utente_id", "data", "ora"}			//questo serve per gestire bene le prenotazioni con data e ora
	        )														//stesso giorno, ore diverse
	    }															//stessa ora, giorni diversi
	)
@EntityListeners(AuditingEntityListener.class)
public class Appuntamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente nutrizionista;

    // Cliente - può essere null se il cliente non è registrato
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    // Campi per gestire clienti non registrati
    @Column(name = "cliente_nome_temp")
    private String clienteNomeTemp;

    @Column(name = "cliente_cognome_temp")
    private String clienteCognomeTemp;


    @Column(nullable = false, name = "descrizione_appuntamento")
    private String descrizioneAppuntamento;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime ora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalita modalita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoAppuntamento stato;

    private String luogo;

    @Column(nullable = false, name = "email_cliente")
    private String emailCliente;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // Enum per modalità
    public enum Modalita {
        ONLINE,
        IN_PRESENZA
    }

    // Enum per stato
    public enum StatoAppuntamento {
        PROGRAMMATO,
        CONFERMATO,
        ANNULLATO
    }

    // Metodo helper per verificare se è un cliente registrato
    public boolean isClienteRegistrato() {
        return cliente != null;
    }

    // Metodo helper per ottenere il nome del cliente
    public String getClienteNomeCompleto() {
        if (cliente != null) {
            return cliente.getNome() + " " + cliente.getCognome();
        }
        return clienteNomeTemp + " " + clienteCognomeTemp;
    }

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utente getNutrizionista() {
        return nutrizionista;
    }

    public void setNutrizionista(Utente nutrizionista) {
        this.nutrizionista = nutrizionista;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getClienteNomeTemp() {
        return clienteNomeTemp;
    }

    public void setClienteNomeTemp(String clienteNomeTemp) {
        this.clienteNomeTemp = clienteNomeTemp;
    }

    public String getClienteCognomeTemp() {
        return clienteCognomeTemp;
    }

    public void setClienteCognomeTemp(String clienteCognomeTemp) {
        this.clienteCognomeTemp = clienteCognomeTemp;
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