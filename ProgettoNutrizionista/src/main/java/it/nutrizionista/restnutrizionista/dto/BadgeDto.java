package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

/** Badge gamification del catalogo, con lo stato di sblocco per il nutrizionista loggato. */
public record BadgeDto(
        String codice,
        String nome,
        String descrizione,
        String icona,
        boolean sbloccato,
        Instant dataSblocco
) {}
