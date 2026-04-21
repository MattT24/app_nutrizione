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
import java.util.Optional;
import java.util.Set;

/**
 * Regola Clinica PoC #3 (Blacklist Personale).
 * Consulta l'elenco delle avversioni registrate dal nutrizionista per il paziente
 * e propaga il livello di allerta impostato (WARNING o ALERT_GRAVE).
 * Il set è già in RAM (pre-fetchato con JOIN FETCH da ClinicalEngineService).
 *
 * @order 100 — Priorità bassa (preferenza personale, non rischio clinico)
 */
@Component
@Order(100)
public class AvversioneRule implements AlimentoRuleValidator {

    @Override
    public ValutazioneClinicaDto valida(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {

        if (blacklistManuale == null || blacklistManuale.isEmpty()) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        // Cerca un'avversione registrata per questo specifico alimento
        Optional<AvversionePersonale> avversione = blacklistManuale.stream()
                .filter(a -> a.getAlimento() != null &&
                             a.getAlimento().getId().equals(alimento.getId()))
                .findFirst();

        if (avversione.isEmpty()) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        AvversionePersonale match = avversione.get();
        String noteTestuale = (match.getNote() != null && !match.getNote().isBlank())
                ? " Note: " + match.getNote()
                : "";

        String emojiGravita = match.getGravita() == LivelloAllerta.ALERT_GRAVE ? "🔴" : "🟡";
        MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                "AVVERSIONE_" + match.getGravita().name(),
                emojiGravita + " Avversione personale registrata per questo alimento." + noteTestuale
        );

        return new ValutazioneClinicaDto(match.getGravita(), List.of(motivo));
    }
}
