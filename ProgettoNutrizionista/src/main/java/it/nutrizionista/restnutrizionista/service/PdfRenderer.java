package it.nutrizionista.restnutrizionista.service;

/**
 * Astrazione del motore di rendering HTML → PDF. L'implementazione di produzione è
 * {@link ChromiumPdfRenderer} (Chromium headless via Playwright); nei test uno stub che ritorna un
 * PDF minimo valido evita di avviare un browser per ogni {@code @SpringBootTest}. La selezione
 * dell'implementazione avviene per property {@code pdf.renderer.impl} (default {@code chromium}).
 */
public interface PdfRenderer {

    /** Genera il PDF a partire dall'HTML già renderizzato da Thymeleaf. Chiamata bloccante. */
    byte[] render(String html);
}
