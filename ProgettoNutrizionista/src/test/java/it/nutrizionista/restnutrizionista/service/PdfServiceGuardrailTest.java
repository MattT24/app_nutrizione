package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.exception.UnprocessableEntityException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica il guardrail sulla dimensione dei documenti (vedi PdfService.assertDimensioneRenderizzabile):
 * senza limite, una scheda con pasti/alimenti illimitati genererebbe un rendering Chromium molto
 * pesante su un endpoint sincrono. Nessun DB/Spring context: il metodo sotto test non ne ha bisogno.
 */
class PdfServiceGuardrailTest {

    // assertDimensioneRenderizzabile non usa nessuna delle altre dipendenze del service
    private final PdfService pdfService = new PdfService(null, null, null, null, null, null);

    @Test
    void schedaEntroILimitiNonSollevaEccezioni() {
        assertDoesNotThrow(() -> pdfService.assertDimensioneRenderizzabile(scheda(3, 5)));
    }

    @Test
    void troppiPastiSollevaUnprocessableEntity() {
        UnprocessableEntityException ex = assertThrows(UnprocessableEntityException.class,
                () -> pdfService.assertDimensioneRenderizzabile(scheda(43, 1)));
        assertTrue(ex.getMessage().contains("pasti"));
    }

    @Test
    void troppiAlimentiSollevaUnprocessableEntity() {
        // 41 pasti (sotto il limite di 42) × 10 alimenti = 410 alimenti (sopra il limite di 400)
        UnprocessableEntityException ex = assertThrows(UnprocessableEntityException.class,
                () -> pdfService.assertDimensioneRenderizzabile(scheda(41, 10)));
        assertTrue(ex.getMessage().contains("alimenti"));
    }

    private Scheda scheda(int numPasti, int alimentiPerPasto) {
        Scheda scheda = new Scheda();
        Set<Pasto> pasti = new LinkedHashSet<>();
        for (int i = 0; i < numPasti; i++) {
            Pasto pasto = new Pasto();
            Set<AlimentoPasto> alimenti = new LinkedHashSet<>();
            for (int j = 0; j < alimentiPerPasto; j++) {
                alimenti.add(new AlimentoPasto());
            }
            pasto.setAlimentiPasto(alimenti);
            pasti.add(pasto);
        }
        scheda.setPasti(pasti);
        return scheda;
    }
}
