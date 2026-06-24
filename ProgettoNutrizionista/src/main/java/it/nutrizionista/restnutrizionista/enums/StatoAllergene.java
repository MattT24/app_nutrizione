package it.nutrizionista.restnutrizionista.enums;

/**
 * Stato tri-stato di un {@link Allergene} su un {@code AlimentoBase}.
 *
 * <p>L'assenza di una entry nella mappa {@code allergeni} equivale a <strong>SCONOSCIUTO</strong>
 * (nessun dato): è semanticamente diverso da {@link #ASSENTE} (verificato libero). Questa
 * distinzione alimenta il livello {@link LivelloAllerta#INFO} ("non verificato") nel motore clinico.
 *
 * <ul>
 *   <li>{@link #PRESENTE}  — l'allergene è contenuto (OFF {@code allergens_tags}).</li>
 *   <li>{@link #TRACCE}    — possibile contaminazione (OFF {@code traces_tags}).</li>
 *   <li>{@link #ASSENTE}   — verificato libero (es. label {@code en:gluten-free}).</li>
 * </ul>
 */
public enum StatoAllergene {
    PRESENTE,
    TRACCE,
    ASSENTE
}
