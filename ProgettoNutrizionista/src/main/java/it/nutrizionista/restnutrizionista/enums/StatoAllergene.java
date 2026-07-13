package it.nutrizionista.restnutrizionista.enums;

/**
 * Stato tri-stato di un {@link Allergene} su un {@code AlimentoBase}.
 *
 * <p>Sia l'assenza di una entry nella mappa {@code allergeni}, sia una entry esplicita
 * {@link #SCONOSCIUTO} equivalgono a <strong>nessun dato</strong>: è semanticamente diverso
 * da {@link #ASSENTE} (verificato libero). Questa distinzione alimenta il livello
 * {@link LivelloAllerta#INFO} ("non verificato") nel motore clinico. Il valore esplicito
 * {@code SCONOSCIUTO} è usato dal seed CREA per gli alimenti senza dati allergeni verificati.
 *
 * <ul>
 *   <li>{@link #PRESENTE}    — l'allergene è contenuto (OFF {@code allergens_tags}).</li>
 *   <li>{@link #TRACCE}      — possibile contaminazione (OFF {@code traces_tags}).</li>
 *   <li>{@link #ASSENTE}     — verificato libero (es. label {@code en:gluten-free}).</li>
 *   <li>{@link #SCONOSCIUTO} — nessun dato verificato (equivalente all'assenza di entry).</li>
 * </ul>
 */
public enum StatoAllergene {
    PRESENTE,
    TRACCE,
    ASSENTE,
    SCONOSCIUTO
}
