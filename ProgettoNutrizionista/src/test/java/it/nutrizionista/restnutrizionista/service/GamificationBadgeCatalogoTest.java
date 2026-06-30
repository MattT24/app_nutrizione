package it.nutrizionista.restnutrizionista.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Unit test puro sul catalogo badge: bordi esatti delle soglie Bronzo/Argento/Oro e coerenza
 * strutturale del catalogo (codici unici, soglie sempre crescenti per gruppo). Non serve alcun
 * mock: {@link GamificationBadgeDefinizione#sbloccato} lavora solo su {@link GamificationContatori}.
 */
class GamificationBadgeCatalogoTest {

    private GamificationBadgeDefinizione trova(String codice) {
        return GamificationBadgeCatalogo.TUTTI.stream()
                .filter(b -> b.codice().equals(codice))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Badge non trovato nel catalogo: " + codice));
    }

    @Test
    void misurazioniBronzo_99misurazioni_nonSbloccato() {
        GamificationContatori contatori = new GamificationContatori(0, 0, 99, 0, 0);
        assertFalse(trova("MISURAZIONI_BRONZO").sbloccato(contatori));
    }

    @Test
    void misurazioniBronzo_100misurazioni_sbloccato() {
        GamificationContatori contatori = new GamificationContatori(0, 0, 100, 0, 0);
        assertTrue(trova("MISURAZIONI_BRONZO").sbloccato(contatori));
    }

    @Test
    void misurazioniOro_499misurazioni_nonSbloccato() {
        GamificationContatori contatori = new GamificationContatori(0, 0, 499, 0, 0);
        assertFalse(trova("MISURAZIONI_ORO").sbloccato(contatori));
    }

    @Test
    void misurazioniOro_500misurazioni_sbloccato() {
        GamificationContatori contatori = new GamificationContatori(0, 0, 500, 0, 0);
        assertTrue(trova("MISURAZIONI_ORO").sbloccato(contatori));
    }

    @Test
    void costanzaBronzo_streakSeiGiorni_nonSbloccato() {
        GamificationContatori contatori = new GamificationContatori(0, 0, 0, 0, 6);
        assertFalse(trova("COSTANZA_BRONZO").sbloccato(contatori));
    }

    @Test
    void costanzaBronzo_streakSetteGiorni_sbloccato() {
        GamificationContatori contatori = new GamificationContatori(0, 0, 0, 0, 7);
        assertTrue(trova("COSTANZA_BRONZO").sbloccato(contatori));
    }

    @Test
    void tuttiICodiciBadge_sonoUnici() {
        List<String> codici = GamificationBadgeCatalogo.TUTTI.stream()
                .map(GamificationBadgeDefinizione::codice)
                .collect(Collectors.toList());
        Set<String> codiciUnici = new HashSet<>(codici);
        assertEquals(codici.size(), codiciUnici.size(), "Ci sono codici badge duplicati nel catalogo");
    }

    @Test
    void ogniGruppo_haEsattamenteTreTierConSoglieCrescenti() {
        var perGruppo = GamificationBadgeCatalogo.TUTTI.stream()
                .collect(Collectors.groupingBy(GamificationBadgeDefinizione::gruppo));

        for (var voce : perGruppo.entrySet()) {
            List<Integer> soglie = voce.getValue().stream()
                    .map(GamificationBadgeDefinizione::soglia)
                    .toList();
            assertEquals(3, soglie.size(), "Il gruppo " + voce.getKey() + " deve avere esattamente 3 tier");
            assertTrue(soglie.get(0) < soglie.get(1) && soglie.get(1) < soglie.get(2),
                    "Le soglie del gruppo " + voce.getKey() + " devono crescere Bronzo < Argento < Oro: " + soglie);
        }
    }
}
