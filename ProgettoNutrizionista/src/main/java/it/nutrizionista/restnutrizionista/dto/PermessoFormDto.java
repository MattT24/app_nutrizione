package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.NotBlank;

/** Form create/update per Permesso (gruppo opzionale). */
public class PermessoFormDto {
    private Long id;

    @NotBlank(message = "Il nome del permesso è obbligatorio")
    private String nome;

    @NotBlank(message = "L'alias del permesso è obbligatorio")
    private String alias;

    private GruppoDto gruppo; // se presente: id valorizzato; null per rimuovere

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public GruppoDto getGruppo() { return gruppo; }
    public void setGruppo(GruppoDto gruppo) { this.gruppo = gruppo; }
}
