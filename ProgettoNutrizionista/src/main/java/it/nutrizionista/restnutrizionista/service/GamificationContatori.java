package it.nutrizionista.restnutrizionista.service;

/** Conteggi reali usati per valutare lo sblocco dei badge gamification del nutrizionista. */
public record GamificationContatori(
        long numClienti,
        long numSchede,
        long numMisurazioni,
        long numAppuntamentiCompletati,
        int streakGiorni
) {}
