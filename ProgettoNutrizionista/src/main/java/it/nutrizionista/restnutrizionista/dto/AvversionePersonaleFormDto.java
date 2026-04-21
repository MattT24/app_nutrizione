package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import jakarta.validation.constraints.NotNull;

public record AvversionePersonaleFormDto(
    @NotNull(message = "L'ID dell'alimento detestato è obbligatorio")
    Long alimentoId,
    
    @NotNull(message = "Specificare il livello della patologia/disgusto")
    LivelloAllerta gravita,
    
    String note
) {}
