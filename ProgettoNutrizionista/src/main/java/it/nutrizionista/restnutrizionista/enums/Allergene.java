package it.nutrizionista.restnutrizionista.enums;

/**
 * I 14 allergeni a dichiarazione obbligatoria dell'Allegato II del Reg. UE 1169/2011.
 *
 * <p>Modello lato <em>alimento</em>, simmetrico ai {@link TagStandard} {@code ALL_*} lato paziente.
 * Lo stato di ciascun allergene su un alimento è rappresentato da {@link StatoAllergene};
 * l'assenza di una entry nella mappa {@code AlimentoBase.allergeni} significa SCONOSCIUTO
 * (dato non disponibile), distinto da {@link StatoAllergene#ASSENTE} (verificato libero).
 *
 * <p>La mappatura verso l'id canonico OpenFoodFacts (es. {@code en:gluten}) e verso il
 * {@link TagStandard} del paziente è centralizzata altrove (mapping statico, vedi piano §3.2).
 */
public enum Allergene {
    GLUTINE,
    CROSTACEI,
    UOVA,
    PESCE,
    ARACHIDI,
    SOIA,
    LATTE,
    FRUTTA_GUSCIO,
    SEDANO,
    SENAPE,
    SESAMO,
    SOLFITI,
    LUPINI,
    MOLLUSCHI
}
