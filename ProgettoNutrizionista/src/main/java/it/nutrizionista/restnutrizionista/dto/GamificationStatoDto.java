package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

/** Stato gamification del nutrizionista loggato: punti, livello, streak di accessi e badge. */
public record GamificationStatoDto(
        int puntiTotali,
        String livelloAttuale,
        String livelloSuccessivo,
        Integer puntiPerLivelloSuccessivo,
        double progressoPercentuale,
        int streakGiorni,
        List<BadgeDto> badge,
        List<CategoriaProgressoDto> progressiCategorie,
        int puntiRiscattabili,
        int sogliaMeseGratis,
        long mesiGratisRiscattati
) {}
