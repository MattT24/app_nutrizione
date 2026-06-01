package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.enums.TagStandard;

/**
 * DTO per il dettaglio cliente (vista anagrafica/clinica).
 *
 * Versione leggera di {@link ClienteDto}: contiene tutti i campi scalari e i
 * TagStandard, ma <b>esclude</b> le relazioni pesanti (misurazioni, plicometrie,
 * schede), il nutrizionista e la blacklist. Quelle liste venivano materializzate
 * via fetch LAZY dal mapper completo pur non essendo usate dalla view, e sono
 * comunque ricaricate in forma mirata dai tab figli (info-cliente).
 */
public record ClienteInfoDto(
        Long id,
        Sesso sesso,
        String nome,
        String cognome,
        String codiceFiscale,
        String email,
        String telefono,
        LocalDate dataNascita,
        Double peso,
        Integer altezza,
        Double pesoTarget,
        Integer altezzaTarget,
        LivelloDiAttivita livelloDiAttivita,
        String intolleranze,
        String funzioniIntestinali,
        String problematicheSalutari,
        String quantitaEQualitaDelSonno,
        String assunzioneFarmaci,
        Boolean beveAlcol,
        Boolean fuma,
        Set<TagStandard> tagStandard,
        Instant createdAt,
        Instant updatedAt
) {}
