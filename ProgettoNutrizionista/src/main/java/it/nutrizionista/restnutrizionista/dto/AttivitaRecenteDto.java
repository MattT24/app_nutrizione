package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

/**
 * DTO di output per il widget "Ultime attività": dati del cliente già joinati
 * con tipo e istante dell'ultima attività.
 */
public record AttivitaRecenteDto(
        Long clienteId,
        String nome,
        String cognome,
        String tipo,
        Instant dataAttivita
) {}
