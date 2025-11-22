package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

/** Richiesta per associare/dissociare UN permesso ad UN ruolo. */
public class RuoloPermessoRequest {
    @NotNull(message = "L'id del ruolo è obbligatorio")
    private Long ruoloId;

    @NotNull(message = "Il permesso è obbligatorio (almeno id)")
    private PermessoDto permesso;

    public Long getRuoloId() { return ruoloId; }
    public void setRuoloId(Long ruoloId) { this.ruoloId = ruoloId; }
    public PermessoDto getPermesso() { return permesso; }
    public void setPermesso(PermessoDto permesso) { this.permesso = permesso; }
}
