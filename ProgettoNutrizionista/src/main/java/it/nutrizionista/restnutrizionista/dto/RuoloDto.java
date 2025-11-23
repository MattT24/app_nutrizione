package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.util.List;

/** DTO di lettura per Ruolo: include SOLO le associazioni Ruolo-Permesso (ruoloPermessi). */
public class RuoloDto {
    private Long id;
    private String nome;
    private String alias;

    // Lista delle associazioni ruolo<->permesso
    private List<PermessoRuoloDto> ruoloPermessi;

    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public List<PermessoRuoloDto> getRuoloPermessi() { return ruoloPermessi; }
    public void setRuoloPermessi(List<PermessoRuoloDto> ruoloPermessi) { this.ruoloPermessi = ruoloPermessi; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
