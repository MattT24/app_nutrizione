package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.enums.TipoDocumento;

/** Accettazione di un documento legale (A4/A9) esposta dall'endpoint di consultazione. */
public record AccettazioneDocumentoDto(
        TipoDocumento tipo,
        String versione,
        Instant accettatoAt,
        Instant revocatoAt
) {}
