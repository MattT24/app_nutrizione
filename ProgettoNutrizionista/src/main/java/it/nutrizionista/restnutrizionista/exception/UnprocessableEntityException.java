package it.nutrizionista.restnutrizionista.exception;

/**
 * Entità sintatticamente valida ma non processabile per regole di dominio.
 * Mappata a HTTP <strong>422 Unprocessable Entity</strong> dal {@code GlobalExceptionHandler}.
 *
 * <p>Uso in Statera: import OpenFoodFacts di un prodotto privo dei 4 macronutrienti
 * obbligatori (kcal/proteine/carboidrati/grassi) — vedi piano E.1.
 * NB: <em>non</em> usare {@code BadRequestException} (che mappa a 400) per questo caso.
 */
public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException(String message) {
        super(message);
    }
}
