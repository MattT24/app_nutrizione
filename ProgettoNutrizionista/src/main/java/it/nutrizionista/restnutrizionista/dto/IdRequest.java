package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotNull;

/** DTO semplice per DELETE con id nel body. */
public class IdRequest {
    @NotNull(message = "L'id è obbligatorio")
    private Long id;

    public IdRequest() {}
    public IdRequest(Long id) { this.id = id; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
