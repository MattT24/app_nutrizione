package it.nutrizionista.restnutrizionista.service;

/**
 * Argomenti gamification: ogni argomento ha tre badge "a crescere" (Bronzo/Argento/Oro, vedi
 * {@link GamificationBadgeCatalogo}) mostrati nel frontend come un solo riquadro che sale di
 * livello. Enum invece di stringhe libere: un typo nel codice non passerebbe la compilazione,
 * invece di fallire silenziosamente a runtime.
 */
public enum GruppoBadge {
    CLIENTI,
    SCHEDE,
    MISURAZIONI,
    APPUNTAMENTI,
    COSTANZA
}
