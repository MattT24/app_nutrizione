package it.nutrizionista.restnutrizionista.service;

import java.util.List;

/**
 * Catalogo statico dei badge gamification disponibili per il nutrizionista. Definito in codice
 * (no admin UI nell'MVP): per aggiungere un badge basta aggiungere una voce a {@link #TUTTI}.
 * Le icone usano le classi PrimeIcons già disponibili nel frontend (es. "pi pi-users").
 *
 * Ogni argomento (clienti, schede, misurazioni, appuntamenti, costanza) ha tre badge "a
 * crescere" — Bronzo, Argento, Oro — con la stessa icona e lo stesso {@code gruppo}, ma soglie
 * via via più alte. Il frontend mostra un solo riquadro per gruppo che sale di livello, usando
 * {@link GamificationService#getStatoPerMe()} (campo {@code progressiCategorie}) per il valore
 * attuale e la soglia del prossimo livello.
 */
public final class GamificationBadgeCatalogo {

    private GamificationBadgeCatalogo() {
    }

    public static final List<GamificationBadgeDefinizione> TUTTI = List.of(
            // ── Clienti (crescita dello studio) ──────────────────────────────
            new GamificationBadgeDefinizione(
                    "CLIENTI_BRONZO", "Clienti Bronzo", "Gestisci 10 clienti",
                    "pi pi-users", GruppoBadge.CLIENTI, 10, GamificationContatori::numClienti),
            new GamificationBadgeDefinizione(
                    "CLIENTI_ARGENTO", "Clienti Argento", "Gestisci 25 clienti",
                    "pi pi-users", GruppoBadge.CLIENTI, 25, GamificationContatori::numClienti),
            new GamificationBadgeDefinizione(
                    "CLIENTI_ORO", "Clienti Oro", "Gestisci 50 clienti",
                    "pi pi-users", GruppoBadge.CLIENTI, 50, GamificationContatori::numClienti),

            // ── Schede dieta ──────────────────────────────────────────────────
            new GamificationBadgeDefinizione(
                    "SCHEDE_BRONZO", "Schede Bronzo", "Crea 10 schede dieta",
                    "pi pi-file", GruppoBadge.SCHEDE, 10, GamificationContatori::numSchede),
            new GamificationBadgeDefinizione(
                    "SCHEDE_ARGENTO", "Schede Argento", "Crea 25 schede dieta",
                    "pi pi-file", GruppoBadge.SCHEDE, 25, GamificationContatori::numSchede),
            new GamificationBadgeDefinizione(
                    "SCHEDE_ORO", "Schede Oro", "Crea 50 schede dieta",
                    "pi pi-file", GruppoBadge.SCHEDE, 50, GamificationContatori::numSchede),

            // ── Misurazioni antropometriche ───────────────────────────────────
            new GamificationBadgeDefinizione(
                    "MISURAZIONI_BRONZO", "Misurazioni Bronzo", "Registra 100 misurazioni antropometriche",
                    "pi pi-chart-line", GruppoBadge.MISURAZIONI, 100, GamificationContatori::numMisurazioni),
            new GamificationBadgeDefinizione(
                    "MISURAZIONI_ARGENTO", "Misurazioni Argento", "Registra 300 misurazioni antropometriche",
                    "pi pi-chart-line", GruppoBadge.MISURAZIONI, 300, GamificationContatori::numMisurazioni),
            new GamificationBadgeDefinizione(
                    "MISURAZIONI_ORO", "Misurazioni Oro", "Registra 500 misurazioni antropometriche",
                    "pi pi-chart-line", GruppoBadge.MISURAZIONI, 500, GamificationContatori::numMisurazioni),

            // ── Appuntamenti completati ───────────────────────────────────────
            new GamificationBadgeDefinizione(
                    "APPUNTAMENTI_BRONZO", "Appuntamenti Bronzo", "Completa 10 appuntamenti",
                    "pi pi-calendar-check", GruppoBadge.APPUNTAMENTI, 10, GamificationContatori::numAppuntamentiCompletati),
            new GamificationBadgeDefinizione(
                    "APPUNTAMENTI_ARGENTO", "Appuntamenti Argento", "Completa 25 appuntamenti",
                    "pi pi-calendar-check", GruppoBadge.APPUNTAMENTI, 25, GamificationContatori::numAppuntamentiCompletati),
            new GamificationBadgeDefinizione(
                    "APPUNTAMENTI_ORO", "Appuntamenti Oro", "Completa 50 appuntamenti",
                    "pi pi-calendar-check", GruppoBadge.APPUNTAMENTI, 50, GamificationContatori::numAppuntamentiCompletati),

            // ── Costanza (streak di accessi giornalieri consecutivi) ──────────
            new GamificationBadgeDefinizione(
                    "COSTANZA_BRONZO", "Costanza Bronzo", "Usa il software per 7 giorni di fila",
                    "pi pi-bolt", GruppoBadge.COSTANZA, 7, c -> c.streakGiorni()),
            new GamificationBadgeDefinizione(
                    "COSTANZA_ARGENTO", "Costanza Argento", "Usa il software per 30 giorni di fila",
                    "pi pi-bolt", GruppoBadge.COSTANZA, 30, c -> c.streakGiorni()),
            new GamificationBadgeDefinizione(
                    "COSTANZA_ORO", "Costanza Oro", "Usa il software per 90 giorni di fila",
                    "pi pi-bolt", GruppoBadge.COSTANZA, 90, c -> c.streakGiorni())
    );
}
