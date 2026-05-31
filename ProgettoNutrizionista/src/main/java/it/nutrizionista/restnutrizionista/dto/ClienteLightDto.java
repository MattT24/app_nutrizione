package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalDate;

import it.nutrizionista.restnutrizionista.entity.Sesso;

/**
 * DTO leggero per la lista-completa dei clienti (statistiche home, widget
 * attività recenti, dropdown e ricerche locali).
 *
 * Sostituisce l'uso di {@link ClienteDto} pesante come tipo di ritorno della
 * lista-completa: prima venivano serializzati 25 campi di cui solo 6 popolati.
 *
 * Superset dei campi già esposti da DtoMapper.toClienteDtoLight (id, nome,
 * cognome, sesso, email, dataNascita) con in più createdAt/updatedAt,
 * necessari all'ordinamento delle attività recenti (prima assenti = bug).
 */
public record ClienteLightDto(
        Long id,
        String nome,
        String cognome,
        Sesso sesso,
        String email,
        LocalDate dataNascita,
        Instant createdAt,
        Instant updatedAt
) {}
