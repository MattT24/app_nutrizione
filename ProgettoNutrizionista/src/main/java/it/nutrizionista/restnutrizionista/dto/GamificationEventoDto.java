package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

/** Evento dello storico punti gamification (pagina "Traguardi"). */
public record GamificationEventoDto(
        String tipoEvento,
        int punti,
        Instant createdAt
) {}
