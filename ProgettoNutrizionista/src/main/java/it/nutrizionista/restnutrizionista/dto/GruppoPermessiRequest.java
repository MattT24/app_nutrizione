package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Associa/dissocia PIÙ permessi ad un gruppo (assegna/rimuove gruppo nei permessi). */
public class GruppoPermessiRequest {
    @NotNull(message = "L'id del gruppo è obbligatorio")
    private Long gruppoId;

    @NotNull(message = "La lista dei permessi è obbligatoria")
    private List<PermessoDto> permessi;

    public Long getGruppoId() { return gruppoId; }
    public void setGruppoId(Long gruppoId) { this.gruppoId = gruppoId; }
    public List<PermessoDto> getPermessi() { return permessi; }
    public void setPermessi(List<PermessoDto> permessi) { this.permessi = permessi; }
}
