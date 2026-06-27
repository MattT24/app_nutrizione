package it.nutrizionista.restnutrizionista.dto;

/**
 * Decisione del nutrizionista su un pasto in conflitto durante l'import (MERGE) di un template:
 * {@code KEEP} lascia invariato il pasto esistente, {@code REPLACE} ne sostituisce il contenuto
 * con quello del template. {@code pastoKey} è la chiave "nome|GIORNO" del pasto.
 */
public record RisoluzioneConflittoDto(String pastoKey, String azione) {}
