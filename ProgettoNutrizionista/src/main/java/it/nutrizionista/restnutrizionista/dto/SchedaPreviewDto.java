package it.nutrizionista.restnutrizionista.dto;

import java.util.List;

/**
 * DTO per l'anteprima scheda: totali nutrizionali precalcolati lato server,
 * senza la lista degli alimenti. Evita di trasferire l'intero albero
 * pasti→alimenti solo per sommare le kcal/macro.
 */
public record SchedaPreviewDto(
        Long id,
        String nome,
        Boolean attiva,
        String tipo,
        List<PastoPreviewDto> pasti
) {}
