package it.nutrizionista.restnutrizionista.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica che il rendering PDF funzioni con il font brand Inter embeddato dal classpath,
 * senza dipendere da DB/contesto Spring. De-risca l'infrastruttura font prima di costruirci sopra i template.
 */
class PdfFontRenderingTest {

    @Test
    void renderizzaPdfValidoConFontInterEmbeddato() throws Exception {
        // renderPdf non usa i repository: si può istanziare il service con dipendenze nulle
        PdfService pdfService = new PdfService(null, null, null, null, null);

        String html = "<html><head><style>body{font-family:'Inter';}</style></head>"
                + "<body><h1 style=\"font-weight:700\">Prova àèìòù €</h1>"
                + "<p style=\"font-weight:600\">Statera Nutrition</p></body></html>";

        byte[] pdf = pdfService.renderPdf(html);

        assertNotNull(pdf, "il PDF non deve essere null");
        assertTrue(pdf.length > 0, "il PDF non deve essere vuoto");

        // Header magico dei file PDF
        String header = new String(pdf, 0, Math.min(5, pdf.length), StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header, "l'output deve essere un PDF valido");

        // Verifica rigorosa dell'embedding: fra i font delle pagine deve comparire Inter
        // (PDFBox espone il nome con eventuale prefisso di subset, es. 'ABCDEF+Inter-Bold')
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            boolean interEmbedded = false;
            for (PDPage page : doc.getPages()) {
                PDResources res = page.getResources();
                if (res == null) continue;
                for (COSName fontName : res.getFontNames()) {
                    PDFont font = res.getFont(fontName);
                    if (font != null && font.getName() != null && font.getName().contains("Inter")) {
                        interEmbedded = true;
                    }
                }
            }
            assertTrue(interEmbedded, "il font Inter deve risultare embeddato nel PDF");
        }
    }
}
