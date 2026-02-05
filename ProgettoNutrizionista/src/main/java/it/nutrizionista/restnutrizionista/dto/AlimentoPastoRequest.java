package it.nutrizionista.restnutrizionista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AlimentoPastoRequest {

    // Usiamo IdRequest (o un DTO simile che ha solo "id")
    // Questo permette al JSON in arrivo di essere { "alimento": { "id": 1 } }
    @NotNull(message = "L'alimento è obbligatorio")
    private IdRequest alimento;

    @NotNull(message = "Il pasto è obbligatorio")
    private IdRequest pasto;

    @Min(value = 1, message = "La quantità deve essere maggiore di 0")
    private int quantita;
    
    private boolean forzaInserimento = false; 

    // Getters e Setters
    public IdRequest getAlimento() { return alimento; }
    public void setAlimento(IdRequest alimento) { this.alimento = alimento; }

    public IdRequest getPasto() { return pasto; }
    public void setPasto(IdRequest pasto) { this.pasto = pasto; }

    public int getQuantita() { return quantita; }
    public void setQuantita(int quantita) { this.quantita = quantita; }

    public boolean isForzaInserimento() { return forzaInserimento; }
    public void setForzaInserimento(boolean forzaInserimento) { this.forzaInserimento = forzaInserimento; }
}