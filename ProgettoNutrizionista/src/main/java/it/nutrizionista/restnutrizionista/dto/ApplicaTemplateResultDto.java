package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

/**
 * Esito dell'applicazione di un template a una scheda.
 * <ul>
 *   <li>{@code applicato=true} → il merge è stato eseguito, {@code scheda} contiene il risultato.</li>
 *   <li>{@code applicato=false} → ci sono conflitti da risolvere ({@code conflitti}); nulla è stato modificato.</li>
 * </ul>
 */
public record ApplicaTemplateResultDto(
        boolean applicato,
        SchedaDto scheda,
        List<PastoConflittoDto> conflitti) {}
