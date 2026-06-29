package it.nutrizionista.restnutrizionista.dto;

/**
 * Macronutrienti essenziali per le viste leggere (template): solo i 4 valori
 * mostrati a schermo. Evita i ~13 campi extra (fibre, zuccheri, sodio, sale,
 * acqua, ...) inutili nelle food card di schede/pasti template e ricette.
 */
public record MacroMiniDto(
        Double calorie,
        Double proteine,
        Double carboidrati,
        Double grassi) {
}
