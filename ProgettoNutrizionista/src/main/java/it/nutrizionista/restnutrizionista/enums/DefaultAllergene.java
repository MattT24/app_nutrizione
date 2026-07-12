package it.nutrizionista.restnutrizionista.enums;

/**
 * Stato allergene <em>di default</em> di un {@code AlimentoBase}: valore applicato agli allergeni
 * per i quali non esiste una entry esplicita nella mappa {@code allergeni}.
 *
 * <p>Serve a distinguere due profili di "non-PRESENTE":
 * <ul>
 *   <li>{@link #ASSENTE}     — l'alimento è considerato libero dagli allergeni non elencati
 *       (es. cereali/verdure CREA senza contaminazioni note) → contributo {@link LivelloAllerta#SAFE}.</li>
 *   <li>{@link #SCONOSCIUTO} — non ci sono dati verificati sugli allergeni non elencati
 *       (es. prodotti trasformati) → contributo {@link LivelloAllerta#INFO} ("non verificato").</li>
 * </ul>
 *
 * <p>Valorizzato dal seed CREA nella colonna {@code alimenti_base.allergeni_default}. Il fallback
 * runtime su questo valore (quando manca la riga in {@code alimento_allergene}) è previsto nel motore
 * clinico in una fase successiva.
 */
public enum DefaultAllergene {
    ASSENTE,
    SCONOSCIUTO
}
