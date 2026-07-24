package it.nutrizionista.restnutrizionista.exception;

/**
 * Fallimento nell'invio di un'email (SMTP irraggiungibile, timeout, credenziali rifiutate).
 * Distinta da {@link PdfGenerationException} per permettere al chiamante (frontend) di
 * distinguere un problema di consegna email da un fallimento del rendering PDF.
 *
 * <p>Mappata a HTTP <strong>502 Bad Gateway</strong> dal {@code GlobalExceptionHandler} —
 * dipendenza esterna (server SMTP) che fallisce.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
