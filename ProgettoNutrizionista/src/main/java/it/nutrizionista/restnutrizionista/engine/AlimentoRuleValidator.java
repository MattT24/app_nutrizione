package it.nutrizionista.restnutrizionista.engine;

import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.enums.TagStandard;

import java.util.Set;

/**
 * Contratto della Chain of Responsibility per la validazione clinica di un alimento.
 * Ogni implementazione riceve esclusivamente dati già caricati in memoria (pre-fetchati
 * dall'orchestratore ClinicalEngineService) per prevenire query N+1 al database.
 *
 * IMPORTANTE: I Set ricevuti sono IMMUTABILI (Set.copyOf). Non tentare di modificarli.
 */
public interface AlimentoRuleValidator {

    /**
     * Valuta se un dato alimento è compatibile per un paziente con i tag e la blacklist forniti.
     *
     * @param alimento           L'alimento da valutare (entità completa con i suoi traccianti).
     * @param tagStandard        Il set IMMUTABILE di tag clinici del paziente (EAGER fetched da Cliente).
     * @param blacklistManuale   Il set IMMUTABILE di avversioni personali del paziente (JOIN FETCH da repository).
     * @return Un record ValutazioneClinicaDto con lo stato e la lista dei motivi.
     */
    ValutazioneClinicaDto valida(
        AlimentoBase alimento,
        Set<TagStandard> tagStandard,
        Set<AvversionePersonale> blacklistManuale
    );
}
