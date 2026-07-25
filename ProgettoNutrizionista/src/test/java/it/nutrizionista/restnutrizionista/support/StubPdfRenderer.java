package it.nutrizionista.restnutrizionista.support;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import it.nutrizionista.restnutrizionista.service.PdfRenderer;

/**
 * Stub del motore PDF per gli integration test: <b>NON avvia Chromium</b>. Ritorna un PDF minimo ma
 * VALIDO (header {@code %PDF-}, apribile da PDFBox) così i test che salvano/scaricano/espongono
 * documenti girano senza browser. Il vero rendering (Chromium/Playwright) resta coperto dai soli test
 * a istanziazione diretta ({@code ChromiumPdfRendererTest}, {@code PdfPreviewGeneratorTest}); il
 * TemplateEngine reale (SpEL) — target di regressione di {@code PdfGenerationIntegrationTest} — gira
 * comunque PRIMA del render, quindi lo stub non ne indebolisce la copertura.
 *
 * <p>Attivo solo con {@code pdf.renderer.impl=stub} (default nel profilo di test, vedi
 * {@code src/test/resources/application.properties}); in produzione la property è assente →
 * {@link it.nutrizionista.restnutrizionista.service.ChromiumPdfRenderer} è l'unico bean
 * {@link PdfRenderer} (matchIfMissing=true).
 */
@Component
@ConditionalOnProperty(prefix = "pdf.renderer", name = "impl", havingValue = "stub")
public class StubPdfRenderer implements PdfRenderer {

    @Override
    public byte[] render(String html) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Stub PDF renderer: generazione del PDF di test fallita", e);
        }
    }
}
