package it.nutrizionista.restnutrizionista.exception;

/**
 * Operazione non consentita perché il cliente è in stato di <b>limitazione del trattamento</b>
 * (art. 18 GDPR, A5.3): il dato resta conservato e leggibile, ma non può essere modificato,
 * prodotto o inviato finché la limitazione è attiva.
 *
 * <p>Mappata a HTTP <strong>423 Locked</strong> dal {@code GlobalExceptionHandler} — semanticamente
 * "la risorsa è bloccata" — distinta dai dinieghi di ownership/permesso (403) e dai conflitti di
 * stato generici (409). Lanciata da {@link it.nutrizionista.restnutrizionista.service.LimitazioneTrattamentoValidator}.
 */
public class TrattamentoLimitatoException extends RuntimeException {

    public TrattamentoLimitatoException(String message) {
        super(message);
    }
}
