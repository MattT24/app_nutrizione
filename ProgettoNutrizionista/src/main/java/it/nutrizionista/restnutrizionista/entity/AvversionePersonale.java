package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.EntityListeners;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "avversione_personale_cliente",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cliente_alimento", 
            columnNames = {"cliente_id", "alimento_base_id"}
        )
    }
)
public class AvversionePersonale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Connette puntualmente all'ID di sistema dell'alimento detestato / allergenico fuori norma
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_base_id", nullable = false)
    private AlimentoBase alimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravita", nullable = false)
    private LivelloAllerta gravita;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // ── A6 — timestamp per il segnale di attività clinica (retention): l'add di un'avversione è
    // attività clinica → createdAt recente mantiene vivo il cliente nella query di eleggibilità. ──
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Costruttori Espliciti ──
    public AvversionePersonale() {
    }

    public AvversionePersonale(Cliente cliente, AlimentoBase alimento, LivelloAllerta gravita, String note) {
        this.cliente = cliente;
        this.alimento = alimento;
        this.gravita = gravita;
        this.note = note;
    }

    // ── Getters / Setters ──
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public AlimentoBase getAlimento() {
        return alimento;
    }

    public void setAlimento(AlimentoBase alimento) {
        this.alimento = alimento;
    }

    public LivelloAllerta getGravita() {
        return gravita;
    }

    public void setGravita(LivelloAllerta gravita) {
        this.gravita = gravita;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvversionePersonale)) return false;
        AvversionePersonale that = (AvversionePersonale) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        // Restituisce un hash fisso della classe per garantire che l'oggetto non "sparisca"
        // dall'HashSet qualora l'ID venga generato dal DB dopo l'inserimento nel Set.
        return getClass().hashCode();
    }
}
