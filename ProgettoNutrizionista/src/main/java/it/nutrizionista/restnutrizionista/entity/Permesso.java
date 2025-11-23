package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entità Permesso: azione/autorità atomica.
 * Collegata ad un Gruppo (ManyToOne) e ad una lista di RuoloPermesso (OneToMany).
 */
@Entity
@Table(name = "permessi")
@EntityListeners(AuditingEntityListener.class)
public class Permesso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String nome;
    @Column(nullable = false, unique = true) private String alias;

    @ManyToOne
    @JoinColumn(name = "gruppo_id")
    private Gruppo gruppo; // opzionale

    /** Lista delle associazioni Ruolo<->Permesso. */
    @OneToMany(mappedBy = "permesso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuoloPermesso> ruoloPermessi = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public Gruppo getGruppo() { return gruppo; }
    public void setGruppo(Gruppo gruppo) { this.gruppo = gruppo; }
    public List<RuoloPermesso> getRuoloPermessi() { return ruoloPermessi; }
    public void setRuoloPermessi(List<RuoloPermesso> ruoloPermessi) { this.ruoloPermessi = ruoloPermessi; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
