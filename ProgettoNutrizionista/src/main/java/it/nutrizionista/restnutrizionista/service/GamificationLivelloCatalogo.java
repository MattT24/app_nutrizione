package it.nutrizionista.restnutrizionista.service;

import java.util.List;

/** Soglie di punti che fanno avanzare di livello il nutrizionista (gamification). Definite in codice. */
public final class GamificationLivelloCatalogo {

    private GamificationLivelloCatalogo() {
    }

    public static final List<GamificationLivello> LIVELLI = List.of(
            new GamificationLivello(0, "Tirocinante"),
            new GamificationLivello(100, "Nutrizionista Junior"),
            new GamificationLivello(300, "Nutrizionista"),
            new GamificationLivello(700, "Nutrizionista Esperto"),
            new GamificationLivello(1500, "Nutrizionista Senior"),
            new GamificationLivello(3000, "Luminare della Nutrizione")
    );

    /** Il livello corrente in base ai punti totali (almeno "Tirocinante", soglia 0). */
    public static GamificationLivello attualePer(int puntiTotali) {
        GamificationLivello corrente = LIVELLI.get(0);
        for (GamificationLivello livello : LIVELLI) {
            if (puntiTotali >= livello.soglia()) {
                corrente = livello;
            } else {
                break;
            }
        }
        return corrente;
    }

    /** Il prossimo livello da raggiungere, o {@code null} se è già stato raggiunto il livello massimo. */
    public static GamificationLivello successivoPer(int puntiTotali) {
        for (GamificationLivello livello : LIVELLI) {
            if (puntiTotali < livello.soglia()) {
                return livello;
            }
        }
        return null;
    }
}
