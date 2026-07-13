package it.nutrizionista.restnutrizionista.exception;

/** Lanciata quando il client supera il limite di tentativi (es. brute-force sul login). Mappa su HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
