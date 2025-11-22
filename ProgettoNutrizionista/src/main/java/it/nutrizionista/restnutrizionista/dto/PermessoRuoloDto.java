package it.nutrizionista.restnutrizionista.dto;

public class PermessoRuoloDto {

    private Long id;
    private RuoloDto ruolo;
    private PermessoDto permesso;

    public PermessoRuoloDto() {}

    public PermessoRuoloDto(Long id, RuoloDto ruolo, PermessoDto permesso) {
        this.id = id;
        this.ruolo = ruolo;
        this.permesso = permesso;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RuoloDto getRuolo() {
        return ruolo;
    }

    public void setRuolo(RuoloDto ruolo) {
        this.ruolo = ruolo;
    }

    public PermessoDto getPermesso() {
        return permesso;
    }

    public void setPermesso(PermessoDto permesso) {
        this.permesso = permesso;
    }
}
