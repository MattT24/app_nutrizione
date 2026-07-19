package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

/**
 * Vista amministrativa essenziale di un cliente registrato da un account demo.
 * I dati sanitari e antropometrici non vengono esposti.
 */
public record ClienteAccountDemoDto(
        Long id,
        String nome,
        String cognome,
        String email,
        String telefono,
        Instant createdAt,
        Instant updatedAt,
        boolean trattamentoLimitato
) {}
