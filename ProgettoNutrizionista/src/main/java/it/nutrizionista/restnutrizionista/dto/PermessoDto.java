package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.util.List;

/** DTO di lettura per Permesso, include il Gruppo e le associazioni RuoloPermesso. */
public class PermessoDto {
    private Long id;
    private String nome;
    private String alias;

    private GruppoDto gruppo;

    // ✅ Lista delle associazioni con ruoli (RuoloPermesso)
    private List<PermessoRuoloDto> ruoloPermessi;

    private Instant createdAt;
    private Instant updatedAt;

    // --- Getter/Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public GruppoDto getGruppo() { return gruppo; }
    public void setGruppo(GruppoDto gruppo) { this.gruppo = gruppo; }

    public List<PermessoRuoloDto> getRuoloPermessi() { return ruoloPermessi; }
    public void setRuoloPermessi(List<PermessoRuoloDto> ruoloPermessi) { this.ruoloPermessi = ruoloPermessi; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
