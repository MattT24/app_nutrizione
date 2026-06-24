package it.nutrizionista.restnutrizionista.exception;

import java.util.Collections;
import java.util.List;

/**
 * Conflitto di stato (HTTP 409). Mappata dal {@code GlobalExceptionHandler}.
 *
 * <p>Due forme di payload, retrocompatibili:
 * <ul>
 *   <li>messaggio semplice (+ eventuale lista conflitti) → body testo (es. duplicati CF/email cliente);</li>
 *   <li>con {@code existingId} valorizzato → body JSON {@code {message, existingId, nome}} così il FE può
 *       offrire "vai all'alimento esistente" (re-import barcode già presente, piano §2.5/§6).</li>
 * </ul>
 */
public class ConflictException extends RuntimeException {

    private final List<String> conflitti;
    private final Long existingId;
    private final String nome;

    public ConflictException(String message) {
        super(message);
        this.conflitti = Collections.emptyList();
        this.existingId = null;
        this.nome = null;
    }

    public ConflictException(String message, List<String> conflitti) {
        super(message);
        this.conflitti = conflitti != null ? conflitti : Collections.emptyList();
        this.existingId = null;
        this.nome = null;
    }

    /** Conflitto "risorsa già esistente": il FE può puntare a {@code existingId}. */
    public ConflictException(String message, Long existingId, String nome) {
        super(message);
        this.conflitti = Collections.emptyList();
        this.existingId = existingId;
        this.nome = nome;
    }

    public List<String> getConflitti() {
        return conflitti;
    }

    public Long getExistingId() {
        return existingId;
    }

    public String getNome() {
        return nome;
    }
}
