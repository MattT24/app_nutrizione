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
 * Regola Clinica: Sensibilità al Glutine Non Celiaca (NCGS).
 * Emette un WARNING (forzabile) se il paziente ha il tag INT_GLUTINE_NCGS
 * e l'alimento contiene glutine.
 *
 * Riciclo logico della detection glutine di AllergiaGlutineRule:
 * 1. Flag booleano senzaGlutine == false
 * 2. Stringa "glutine" presente nel Set<String> tracce dell'alimento
 *
 * La differenza rispetto ad AllergiaGlutineRule è la severità:
 * - ALL_GLUTINE (celiachia) → ALERT_GRAVE (blocco totale, rischio autoimmune)
 * - INT_GLUTINE_NCGS (NCGS) → WARNING (avviso forzabile, disagio non letale)
 *
 * Se un paziente ha ENTRAMBI i tag, il ClinicalEngineService prenderà
 * il livello più alto (ALERT_GRAVE), quindi la celiachia prevale sulla NCGS.
 *
 * @order 21 — Subito dopo IntolleranzaLattosioRule
 */
@Component
@Order(21)
public class IntolleranzaNcgsRule implements AlimentoRuleValidator {

    private static final String TRACCIA_GLUTINE = "glutine";

    @Override
    public ValutazioneClinicaDto valida(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {

        // Regola applicabile SOLO ai pazienti con tag NCGS
        if (!tagStandard.contains(TagStandard.INT_GLUTINE_NCGS)) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        // Check 1: Flag booleano esplicito (fonte più affidabile)
        if (alimento.getSenzaGlutine() != null && !alimento.getSenzaGlutine()) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "INT_GLUTINE_NCGS",
                    "🟡 Questo alimento CONTIENE GLUTINE (flag verificato). Il paziente ha sensibilità al glutine non celiaca (Tag: INT_GLUTINE_NCGS)."
            );
            return new ValutazioneClinicaDto(LivelloAllerta.WARNING, List.of(motivo));
        }

        // Check 2: Ricerca nei traccianti testuali (fallback per dati CREA legacy)
        boolean contieneGlutine = alimento.getTracce() != null &&
                alimento.getTracce().stream()
                        .anyMatch(traccia -> traccia.toLowerCase().contains(TRACCIA_GLUTINE));

        if (contieneGlutine) {
            MotivoValutazioneDto motivo = new MotivoValutazioneDto(
                    "INT_GLUTINE_NCGS",
                    "🟡 Questo alimento contiene GLUTINE nei traccianti. Il paziente ha sensibilità al glutine non celiaca (Tag: INT_GLUTINE_NCGS)."
            );
            return new ValutazioneClinicaDto(LivelloAllerta.WARNING, List.of(motivo));
        }

        return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
    }
}
