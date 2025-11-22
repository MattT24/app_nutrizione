package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.util.List;

/** DTO di lettura per Gruppo, include i permessi (opzionale). */
public class GruppoDto {
    private Long id;
    private String nome;
    private String alias;
    private List<PermessoDto> permessi;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public List<PermessoDto> getPermessi() { return permessi; }
    public void setPermessi(List<PermessoDto> permessi) { this.permessi = permessi; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
