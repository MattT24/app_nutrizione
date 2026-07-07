package it.nutrizionista.restnutrizionista.dto;

/** Un premio riscattabile con i punti gamification: costo, testi UI e stato del mese corrente. */
public record PremioDisponibileDto(
        String tipo,
        String titolo,
        String descrizione,
        int costo,
        long volteRiscattato,
        boolean giaRiscattatoQuestomese
) {}
