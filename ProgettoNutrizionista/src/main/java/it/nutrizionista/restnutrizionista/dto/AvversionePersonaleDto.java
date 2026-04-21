package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;

public record AvversionePersonaleDto(
    Long id,
    Long alimentoId,          // Chiave e ID referenza rapida per UI Angular
    String alimentoNome,      // Comodo per la visualizzazione nella UI Nutrizionista 
    LivelloAllerta gravita,
    String note
) {}
