package it.nutrizionista.restnutrizionista.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import it.nutrizionista.restnutrizionista.dto.MotivoValutazioneDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;

/**
 * Regola clinica <strong>data-driven</strong> per i 14 allergeni UE + le intolleranze
 * glutine (NCGS) e lattosio. Sostituisce le 3 regole hardcoded
 * (AllergiaGlutineRule/IntolleranzaLattosioRule/IntolleranzaNcgsRule) leggendo la mappa tri-stato
 * {@code AlimentoBase.allergeni}.
 *
 * <p>Attiva solo con {@code clinica.engine.use-allergen-rule=true} (feature flag di transizione,
 * piano E.11). Quando attiva, le 3 regole legacy sono disattivate dallo stesso flag.
 *
 * <p>Mapping tag→esito (piano §3.1):
 * <pre>
 *  Allergia ALL_*    PRESENTE→ALERT_GRAVE  TRACCE→WARNING  ASSENTE→SAFE  SCONOSCIUTO→INFO
 *  Intolleranza INT_ presente→WARNING                                    SCONOSCIUTO→INFO
 * </pre>
 *
 * @order 1 — priorità massima (rischio anafilattico/autoimmune), come AllergiaGlutineRule.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "clinica.engine.use-allergen-rule", havingValue = "true")
public class AllergeneRule implements AlimentoRuleValidator {

    private static final List<String> LOW_LACTOSE_KEYWORDS =
            List.of("stagionat", "burro", "parmigian", "grana", "pecorino");

    @Override
    public ValutazioneClinicaDto valida(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {

        if (alimento == null || tagStandard == null || tagStandard.isEmpty()) {
            return new ValutazioneClinicaDto(LivelloAllerta.SAFE, List.of());
        }

        Map<Allergene, StatoAllergene> stati = alimento.getAllergeni();
        if (stati == null) stati = Map.of();

        List<MotivoValutazioneDto> motivi = new ArrayList<>();
        LivelloAllerta peggiore = LivelloAllerta.SAFE;

        for (TagStandard tag : tagStandard) {
            // ── Allergie ALL_* ──
            Allergene allergene = TagStandardAllergeneMapping.allergeneFor(tag);
            if (allergene != null) {
                StatoAllergene stato = stati.get(allergene);
                String label = TagStandardAllergeneMapping.label(allergene);
                LivelloAllerta lvl = null;
                String msg = null;
                if (stato == StatoAllergene.PRESENTE) {
                    lvl = LivelloAllerta.ALERT_GRAVE;
                    msg = "🔴 Contiene " + label + " (allergene dichiarato). Il paziente è allergico (Tag: " + tag.name() + ").";
                } else if (stato == StatoAllergene.TRACCE) {
                    lvl = LivelloAllerta.WARNING;
                    msg = "🟡 Può contenere tracce di " + label + ". Il paziente è allergico (Tag: " + tag.name() + ").";
                } else if (stato == StatoAllergene.ASSENTE) {
                    continue; // SAFE
                } else {
                    lvl = LivelloAllerta.INFO;
                    msg = "ⓘ Presenza di " + label + " non verificata per questo prodotto (Tag: " + tag.name() + ").";
                }
                motivi.add(new MotivoValutazioneDto(tag.name(), msg));
                if (ordine(lvl) > ordine(peggiore)) peggiore = lvl;
                continue;
            }

            // ── Intolleranza NCGS (glutine) ──
            if (tag == TagStandard.INT_GLUTINE_NCGS) {
                StatoAllergene g = stati.get(Allergene.GLUTINE);
                if (g == StatoAllergene.PRESENTE) {
                    motivi.add(new MotivoValutazioneDto("INT_GLUTINE_NCGS",
                            "🟡 Contiene glutine. Il paziente ha sensibilità al glutine non celiaca (Tag: INT_GLUTINE_NCGS)."));
                    if (ordine(LivelloAllerta.WARNING) > ordine(peggiore)) peggiore = LivelloAllerta.WARNING;
                } else if (g == null || g == StatoAllergene.TRACCE) {
                    motivi.add(new MotivoValutazioneDto("INT_GLUTINE_NCGS",
                            "ⓘ Presenza di glutine non verificata per questo prodotto (Tag: INT_GLUTINE_NCGS)."));
                    if (ordine(LivelloAllerta.INFO) > ordine(peggiore)) peggiore = LivelloAllerta.INFO;
                }
                continue;
            }

            // ── Intolleranza al lattosio (LATTE ≠ lattosio: helper) ──
            if (tag == TagStandard.INT_LATTOSIO) {
                StatoAllergene latte = stati.get(Allergene.LATTE);
                if (latte == null) {
                    motivi.add(new MotivoValutazioneDto("INT_LATTOSIO",
                            "ⓘ Presenza di lattosio non verificata per questo prodotto (Tag: INT_LATTOSIO)."));
                    if (ordine(LivelloAllerta.INFO) > ordine(peggiore)) peggiore = LivelloAllerta.INFO;
                } else if (lattosioPresente(alimento, latte)) {
                    motivi.add(new MotivoValutazioneDto("INT_LATTOSIO",
                            "🟡 Contiene lattosio. Il paziente è intollerante al lattosio (Tag: INT_LATTOSIO)."));
                    if (ordine(LivelloAllerta.WARNING) > ordine(peggiore)) peggiore = LivelloAllerta.WARNING;
                }
                // altrimenti (lactose-free o stagionato/burro o LATTE ASSENTE) → SAFE
            }
        }

        return new ValutazioneClinicaDto(peggiore, List.copyOf(motivi));
    }

    /** Lattosio presente = LATTE presente/tracce AND non lactose-free AND non low-lactose per nome (E.9). */
    private boolean lattosioPresente(AlimentoBase alimento, StatoAllergene latte) {
        boolean hasLatte = latte == StatoAllergene.PRESENTE || latte == StatoAllergene.TRACCE;
        if (!hasLatte) return false;
        if (Boolean.TRUE.equals(alimento.getSenzaLattosio())) return false;
        String nome = alimento.getNome() != null ? alimento.getNome().toLowerCase() : "";
        for (String kw : LOW_LACTOSE_KEYWORDS) {
            if (nome.contains(kw)) return false;
        }
        return true;
    }

    private int ordine(LivelloAllerta livello) {
        return switch (livello) {
            case SAFE        -> 0;
            case INFO        -> 1;
            case WARNING     -> 2;
            case ALERT_GRAVE -> 3;
        };
    }
}
