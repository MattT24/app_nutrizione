package it.nutrizionista.restnutrizionista.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entità Gruppo: colleziona Permessi omogenei.
 * Ha una lista di permessi (OneToMany).
 */
@Entity
@Table(name = "gruppi")
@EntityListeners(AuditingEntityListener.class)
public class Gruppo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String nome;
    @Column(nullable = false, unique = true) private String alias;

    /** Lista di permessi appartenenti al gruppo. */
    @OneToMany(mappedBy = "gruppo", cascade = CascadeType.ALL)
    private List<Permesso> permessi = new ArrayList<>();

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
    public List<Permesso> getPermessi() { return permessi; }
    public void setPermessi(List<Permesso> permessi) { this.permessi = permessi; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
