package it.nutrizionista.restnutrizionista.service;

import java.util.function.ToLongFunction;

/**
 * Definizione statica di un badge gamification: codice, presentazione, gruppo di appartenenza
 * (es. "MISURAZIONI") e soglia di sblocco sul contatore estratto da {@link #valore()}. I badge
 * dello stesso gruppo formano una scala Bronzo/Argento/Oro (stessa icona, soglie crescenti).
 */
public record GamificationBadgeDefinizione(
        String codice,
        String nome,
        String descrizione,
        String icona,
        GruppoBadge gruppo,
        int soglia,
        ToLongFunction<GamificationContatori> valore
) {
    public boolean sbloccato(GamificationContatori contatori) {
        return valore.applyAsLong(contatori) >= soglia;
    }
}
