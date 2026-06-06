package it.nutrizionista.restnutrizionista.engine;

import it.nutrizionista.restnutrizionista.dto.MotivoValutazioneDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Regola Clinica PoC #1 (Booleana).
 * Blocca con ALERT_GRAVE se il paziente è celiaco e l'alimento contiene glutine.
 *
 * Logica di rilevamento multi-sorgente:
 * 1. Flag booleano senzaGlutine == false (dati verificati da nutrizionista o import OFF)
 * 2. Stringa "glutine" presente nel Set<String> tracce dell'alimento
 *
 * @order 1 — Priorità massima (rischio anafilattico/autoimmune)
 */
@Component
@Order(1)
public class AllergiaGlutineRule implements AlimentoRuleValidator {

    private static final String TRACCIA_GLUTINE = "glutine";

    @Override
    public ValutazioneClinicaDto valida(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {

        // Regola applicabile SOLO ai pazienti con tag celiaco
        if (!tagStandard.contains(TagStandard.ALL_GLUTINE)) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        // Check 1: Flag booleano esplicito (fonte più affidabile)
        if (alimento.getSenzaGlutine() != null && !alimento.getSenzaGlutine()) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "ALL_GLUTINE",
                    "⚠️ Questo alimento CONTIENE GLUTINE (flag verificato). Il paziente è celiaco (Tag: ALL_GLUTINE)."
            );
            return new ValutazioneClinicaDto(LivelloAllerta.ALERT_GRAVE, List.of(motivo));
        }

        // Check 2: Ricerca nei traccianti testuali (fallback per dati CREA legacy)
        boolean contieneGlutine = alimento.getTracce() != null &&
                alimento.getTracce().stream()
                        .anyMatch(traccia -> traccia.toLowerCase().contains(TRACCIA_GLUTINE));

        if (contieneGlutine) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "ALL_GLUTINE",
                    "⚠️ Questo alimento contiene GLUTINE nei traccianti. Il paziente è celiaco (Tag: ALL_GLUTINE)."
            );
            return new ValutazioneClinicaDto(LivelloAllerta.ALERT_GRAVE, List.of(motivo));
        }

        return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
    }
}
