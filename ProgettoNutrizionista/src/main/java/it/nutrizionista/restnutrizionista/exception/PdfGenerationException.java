package it.nutrizionista.restnutrizionista.exception;

/**
 * Fallimento del motore di rendering PDF (Chromium headless): crash del browser, timeout di
 * generazione, o pool di worker saturo. Distinta da {@link EmailDeliveryException} per permettere
 * al chiamante (frontend) di distinguere un problema del nostro sottosistema di rendering da un
 * fallimento della consegna email.
 *
 * <p>Mappata a HTTP <strong>503 Service Unavailable</strong> dal {@code GlobalExceptionHandler} —
 * condizione transitoria del nostro sottosistema, ragionevolmente ritentabile dal chiamante.
 */
public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message) {
        super(message);
    }

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
