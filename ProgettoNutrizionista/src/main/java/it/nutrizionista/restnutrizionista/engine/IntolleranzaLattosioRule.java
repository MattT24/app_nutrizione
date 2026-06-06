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
 * Regola Clinica: Intolleranza al Lattosio (Carenza enzimatica lattasi).
 * Emette un WARNING (forzabile) se il paziente ha il tag INT_LATTOSIO
 * e l'alimento contiene lattosio.
 *
 * Logica di rilevamento multi-sorgente:
 * 1. Flag booleano senzaLattosio == false (dati verificati da nutrizionista o import OFF)
 * 2. Stringa "lattosio" presente nel Set<String> tracce dell'alimento
 *
 * A differenza di un'allergia (ALERT_GRAVE), l'intolleranza produce un
 * WARNING che il nutrizionista può forzare con forzaInserimento=true.
 *
 * @order 20 — Priorità alta (dopo ipertensione, prima delle avversioni personali)
 */
@Component
@Order(20)
public class IntolleranzaLattosioRule implements AlimentoRuleValidator {

    private static final String TRACCIA_LATTOSIO = "lattosio";

    @Override
    public ValutazioneClinicaDto valida(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {

        // Regola applicabile SOLO ai pazienti con tag intolleranza lattosio
        if (!tagStandard.contains(TagStandard.INT_LATTOSIO)) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        // Check 1: Flag booleano esplicito (fonte più affidabile)
        if (alimento.getSenzaLattosio() != null && !alimento.getSenzaLattosio()) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "INT_LATTOSIO",
                    "🟡 Questo alimento CONTIENE LATTOSIO (flag verificato). Il paziente è intollerante al lattosio (Tag: INT_LATTOSIO)."
            );
            return new ValutazioneClinicaDto(LivelloAllerta.WARNING, List.of(motivo));
        }

        // Check 2: Ricerca nei traccianti testuali (fallback per dati CREA legacy)
        boolean contieneLattosio = alimento.getTracce() != null &&
                alimento.getTracce().stream()
                        .anyMatch(traccia -> traccia.toLowerCase().contains(TRACCIA_LATTOSIO));

        if (contieneLattosio) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "INT_LATTOSIO",
                    "🟡 Questo alimento contiene LATTOSIO nei traccianti. Il paziente è intollerante al lattosio (Tag: INT_LATTOSIO)."
            );
            return new ValutazioneClinicaDto(LivelloAllerta.WARNING, List.of(motivo));
        }

        return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
    }
}
