package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

/**
 * Conflitto rilevato durante l'import (MERGE) di un template scheda: un pasto del template ha
 * un omonimo (stesso nome+giorno) già presente nella scheda e con alimenti. Le due liste di nomi
 * servono al frontend per mostrare cosa verrebbe sostituito.
 */
public record PastoConflittoDto(
        String pastoKey,
        String nome,
        String giorno,
        List<String> alimentiAttuali,
        List<String> alimentiTemplate) {}
