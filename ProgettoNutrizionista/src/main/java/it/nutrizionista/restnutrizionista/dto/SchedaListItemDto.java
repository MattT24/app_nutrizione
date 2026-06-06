package it.nutrizionista.restnutrizionista.dto;

import java.time.LocalDate;

/**
 * DTO per la riga della lista schede di un cliente: solo i campi mostrati
 * (nome, data, tipo, stato, numero pasti). Esclude l'oggetto cliente, i pasti
 * e i timestamp, non necessari nella lista (il clienteId è noto dal contesto).
 */
public record SchedaListItemDto(
        Long id,
        String nome,
        LocalDate dataCreazione,
        String tipo,
        Boolean attiva,
        Integer numeroPasti
) {}
