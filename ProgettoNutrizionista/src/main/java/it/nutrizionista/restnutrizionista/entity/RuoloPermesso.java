package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


/**
 * Entità join Ruolo <-> Permesso.
 * Chiave primaria autonoma (Long id), ma garantisce unicità della coppia (ruolo, permesso)
 */
@Entity
@Table(
    name = "ruoli_permessi",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ruolo_permesso", columnNames = {"ruolo_id", "permesso_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class RuoloPermesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Un Ruolo per ogni riga. */
    @ManyToOne
    @JoinColumn(name = "ruolo_id", nullable = false)
    private Ruolo ruolo;

    /** Un Permesso per ogni riga. */
    @ManyToOne
    @JoinColumn(name = "permesso_id", nullable = false)
    private Permesso permesso;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // === Costruttori ===
    public RuoloPermesso() {}
    
    


    public RuoloPermesso(Ruolo ruolo, Permesso permesso) {
		this.ruolo = ruolo;
		this.permesso = permesso;
	}


	// === Getter & Setter ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Ruolo getRuolo() { return ruolo; }
    public void setRuolo(Ruolo ruolo) { this.ruolo = ruolo; }

    public Permesso getPermesso() { return permesso; }
    public void setPermesso(Permesso permesso) { this.permesso = permesso; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
