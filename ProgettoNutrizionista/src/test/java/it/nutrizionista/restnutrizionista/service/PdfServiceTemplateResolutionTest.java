package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TemplatePdf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica le priorità di risoluzione del template PDF delle schede (vedi PdfService.resolveTemplatePdf):
 * override esplicito > preferenza salvata sul nutrizionista > default DETTAGLIATO. Nessun DB/Spring
 * context: il metodo sotto test non ne ha bisogno (stesso pattern di PdfServiceGuardrailTest).
 */
class PdfServiceTemplateResolutionTest {

    private final PdfService pdfService = new PdfService(null, null, null, null, null, null);

    @Test
    void overrideEsplicitoVinceSuTutto() {
        Scheda scheda = schedaConPreferenza(TemplatePdf.ESSENZIALE);
        assertEquals(TemplatePdf.DETTAGLIATO, pdfService.resolveTemplatePdf(scheda, TemplatePdf.DETTAGLIATO));
    }

    @Test
    void senzaOverrideUsaLaPreferenzaDelNutrizionista() {
        Scheda scheda = schedaConPreferenza(TemplatePdf.ESSENZIALE);
        assertEquals(TemplatePdf.ESSENZIALE, pdfService.resolveTemplatePdf(scheda, null));
    }

    @Test
    void senzaPreferenzaSalvataUsaDettagliatoComeDefault() {
        Scheda scheda = schedaConPreferenza(null);
        assertEquals(TemplatePdf.DETTAGLIATO, pdfService.resolveTemplatePdf(scheda, null));
    }

    @Test
    void senzaClienteUsaDettagliatoComeDefault() {
        Scheda scheda = new Scheda(); // nessun cliente collegato
        assertEquals(TemplatePdf.DETTAGLIATO, pdfService.resolveTemplatePdf(scheda, null));
    }

    private Scheda schedaConPreferenza(TemplatePdf preferenza) {
        Utente nutrizionista = new Utente();
        nutrizionista.setTemplatePdfPreferito(preferenza);
        Cliente cliente = new Cliente();
        cliente.setNutrizionista(nutrizionista);
        Scheda scheda = new Scheda();
        scheda.setCliente(cliente);
        return scheda;
    }
}
