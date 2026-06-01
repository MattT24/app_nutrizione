package it.nutrizionista.restnutrizionista.dto;

/**
 * Pasto in anteprima: solo nome, giorno (per le settimanali) e totali
 * nutrizionali precalcolati lato server. Niente lista alimenti.
 */
public record PastoPreviewDto(
        String nome,
        String giorno,
        double kcal,
        double proteineG,
        double carboidratiG,
        double grassiG
) {}
