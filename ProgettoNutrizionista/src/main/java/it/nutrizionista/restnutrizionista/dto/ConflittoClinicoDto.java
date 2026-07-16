package it.nutrizionista.restnutrizionista.dto;

import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;

/**
 * Conflitto clinico (non-SAFE) di un singolo alimento valutato contro un paziente target, usato dai
 * percorsi batch (applicazione template / duplicazione scheda) per riepilogare i conflitti prima
 * dell'inserimento — finding F-D1a.
 *
 * @param alimentoId          id dell'{@link it.nutrizionista.restnutrizionista.entity.AlimentoBase}
 * @param nome                nome dell'alimento (per il riepilogo)
 * @param livello             livello aggregato ({@code WARNING} o {@code ALERT_GRAVE})
 * @param motivi              concatenazione dei messaggi dei motivi non-SAFE
 * @param allergeneDichiarato true se un motivo deriva da un allergene dichiarato del paziente
 *                            (caso potenzialmente letale, da distinguere visivamente dagli altri gravi)
 */
public record ConflittoClinicoDto(
        Long alimentoId,
        String nome,
        LivelloAllerta livello,
        String motivi,
        boolean allergeneDichiarato) {
}
