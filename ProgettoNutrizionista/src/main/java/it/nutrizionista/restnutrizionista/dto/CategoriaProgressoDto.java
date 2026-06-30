package it.nutrizionista.restnutrizionista.dto;

/**
 * Progresso del nutrizionista in un singolo argomento gamification (es. "Misurazioni"): un solo
 * riquadro che sale di livello (Bronzo → Argento → Oro) invece di tre badge separati.
 */
public record CategoriaProgressoDto(
        String chiave,
        String titolo,
        String icona,
        long valoreAttuale,
        String tierAttuale,
        Integer sogliaProssimoTier,
        String nomeProssimoTier
) {}
