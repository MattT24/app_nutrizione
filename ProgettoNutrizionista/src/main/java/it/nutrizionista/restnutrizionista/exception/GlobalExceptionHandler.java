package it.nutrizionista.restnutrizionista.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authorization.AuthorizationDeniedException;

/**
 * Gestione centralizzata delle eccezioni comuni.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
    
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleAuthorizationDenied(AuthorizationDeniedException ex) {
    	return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Rete di sicurezza per violazioni di vincoli DB. I duplicati di codice fiscale/email
        // sono già intercettati a monte con messaggi specifici (ConflictException); qui finiscono
        // gli altri casi (es. campo NOT NULL non valorizzato), quindi il messaggio resta neutro.
        String causa = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        if (causa != null && (causa.toLowerCase().contains("duplicate") || causa.toLowerCase().contains("unique"))) {
            // Messaggio neutro: i duplicati di codice fiscale/email del cliente hanno già un messaggio
            // dedicato (ConflictException) a monte; qui arriva qualsiasi altra violazione di unicità.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Operazione non riuscita: esiste già un elemento con questi dati (vincolo di unicità violato).");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Impossibile salvare: alcuni dati obbligatori non sono validi o mancanti.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuth(AuthenticationException ex) {
    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a,b) -> a + "; " + b)
                .orElse("Validazione fallita");
        return ResponseEntity.badRequest().body("Errore di validazione: " + msg);
    }
}
