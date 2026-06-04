package it.nutrizionista.restnutrizionista.dto;

/**
 * Request minimale per la rinomina (titolo) di un'entità.
 * {@code nome} vuoto/blank è ammesso e azzera il titolo lato service.
 */
public record RinominaRequest(String nome) {}
