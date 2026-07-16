package it.nutrizionista.restnutrizionista.exception;

import java.util.Collections;
import java.util.List;

import it.nutrizionista.restnutrizionista.dto.ConflittoClinicoDto;

/**
 * Conflitto di stato (HTTP 409). Mappata dal {@code GlobalExceptionHandler}.
 *
 * <p>Forme di payload, retrocompatibili:
 * <ul>
 *   <li>messaggio semplice (+ eventuale lista conflitti testuali) → body testo (es. duplicati CF/email cliente);</li>
 *   <li>con {@code existingId} valorizzato → body JSON {@code {message, existingId, nome}} così il FE può
 *       offrire "vai all'alimento esistente" (re-import barcode già presente);</li>
 *   <li>con {@code conflittiClinici} valorizzato → body JSON {@code {message, conflittiClinici[]}} per il gate
 *       block-and-report dei percorsi template (finding F-D1a): il FE mostra la lista per-item (gravi con toggle
 *       "includi consapevolmente", allergeni distinti; warning come info) e ri-invia con la decisione.</li>
 * </ul>
 */
public class ConflictException extends RuntimeException {

    private final List<String> conflitti;
    private final Long existingId;
    private final String nome;
    private final List<ConflittoClinicoDto> conflittiClinici;

    public ConflictException(String message) {
        this(message, Collections.emptyList(), null, null, Collections.emptyList());
    }

    public ConflictException(String message, List<String> conflitti) {
        this(message, conflitti != null ? conflitti : Collections.emptyList(), null, null, Collections.emptyList());
    }

    /** Conflitto "risorsa già esistente": il FE può puntare a {@code existingId}. */
    public ConflictException(String message, Long existingId, String nome) {
        this(message, Collections.emptyList(), existingId, nome, Collections.emptyList());
    }

    private ConflictException(String message, List<String> conflitti, Long existingId, String nome,
                              List<ConflittoClinicoDto> conflittiClinici) {
        super(message);
        this.conflitti = conflitti;
        this.existingId = existingId;
        this.nome = nome;
        this.conflittiClinici = conflittiClinici;
    }

    /**
     * Conflitti clinici (finding F-D1a): il body 409 porta la lista strutturata per-item
     * (con {@code livello}/{@code allergeneDichiarato}) per il riepilogo e la risoluzione consapevole.
     */
    public static ConflictException clinici(String message, List<ConflittoClinicoDto> conflittiClinici) {
        return new ConflictException(message, Collections.emptyList(), null, null,
                conflittiClinici != null ? conflittiClinici : Collections.emptyList());
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

    public List<ConflittoClinicoDto> getConflittiClinici() {
        return conflittiClinici;
    }
}
