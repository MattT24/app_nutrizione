package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Traccia l'ultima attività del nutrizionista su un cliente (widget "Ultime attività").
 * Una sola riga per coppia (nutrizionista, cliente): viene aggiornata in upsert ad ogni
 * interazione, mantenendo tipo e timestamp più recenti. Sostituisce il vecchio tracciamento
 * in localStorage del frontend.
 */
@Entity
@Table(
    name = "attivita_recenti",
    uniqueConstraints = @UniqueConstraint(columnNames = {"utente_id", "cliente_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class AttivitaRecente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utente_id")
    private Utente nutrizionista;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Tipo di attività (es. "Misurazione", "Scheda Dieta", "Nuovo Cliente"). */
    @Column(nullable = false)
    private String tipo;

    /** Istante dell'ultima attività: mostrato nel widget e usato per l'ordinamento. */
    @Column(name = "data_attivita", nullable = false)
    private Instant dataAttivita;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Instant getDataAttivita() {
        return dataAttivita;
    }

    public void setDataAttivita(Instant dataAttivita) {
        this.dataAttivita = dataAttivita;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
