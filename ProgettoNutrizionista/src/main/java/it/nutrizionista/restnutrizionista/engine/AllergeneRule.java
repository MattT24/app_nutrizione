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
import it.nutrizionista.restnutrizionista.enums.DefaultAllergene;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;

/**
 * Regola clinica <strong>data-driven</strong> per i 14 allergeni UE + le intolleranze
 * glutine (NCGS) e lattosio. È il motore allergeni di default: legge la mappa tri-stato
 * {@code AlimentoBase.allergeni} (sostituisce le vecchie regole hardcoded, ora rimosse).
 *
 * <p>Attiva di default: {@code @ConditionalOnProperty(... matchIfMissing=true)}, con override
 * esplicito {@code clinica.engine.use-allergen-rule=true} in {@code application.properties}.
 *
 * <p><strong>Risoluzione Opzione C</strong> (ponte con lo storage Fase 2): per ogni
 * {@code (alimento, allergene)} lo stato è quello della riga esplicita in {@code alimento_allergene};
 * se la riga manca si usa {@code alimenti_base.allergeni_default} — vedi {@link #effettivo}. Così i
 * ~700 alimenti con default ASSENTE danno SAFE (non INFO) per gli allergeni non elencati.
 *
 * <p>Mapping tag→esito (piano §3.1):
 * <pre>
 *  Allergia ALL_*    PRESENTE→ALERT_GRAVE  TRACCE→WARNING  ASSENTE→SAFE  SCONOSCIUTO→INFO
 *  Intolleranza INT_ presente→WARNING                                    SCONOSCIUTO→INFO
 * </pre>
 *
 * @order 1 — priorità massima (rischio anafilattico/autoimmune).
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "clinica.engine.use-allergen-rule", havingValue = "true", matchIfMissing = true)
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
                StatoAllergene stato = effettivo(stati, alimento, allergene);
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

            // ── Intolleranza NCGS (glutine) ── (Opzione C: riga esplicita ?? allergeni_default)
            if (tag == TagStandard.INT_GLUTINE_NCGS) {
                StatoAllergene g = effettivo(stati, alimento, Allergene.GLUTINE);
                if (g == StatoAllergene.PRESENTE) {
                    motivi.add(new MotivoValutazioneDto("INT_GLUTINE_NCGS",
                            "🟡 Contiene glutine. Il paziente ha sensibilità al glutine non celiaca (Tag: INT_GLUTINE_NCGS)."));
                    if (ordine(LivelloAllerta.WARNING) > ordine(peggiore)) peggiore = LivelloAllerta.WARNING;
                } else if (g == StatoAllergene.TRACCE || g == StatoAllergene.SCONOSCIUTO) {
                    motivi.add(new MotivoValutazioneDto("INT_GLUTINE_NCGS",
                            "ⓘ Presenza di glutine non verificata per questo prodotto (Tag: INT_GLUTINE_NCGS)."));
                    if (ordine(LivelloAllerta.INFO) > ordine(peggiore)) peggiore = LivelloAllerta.INFO;
                }
                // ASSENTE → SAFE
                continue;
            }

            // ── Intolleranza al lattosio (LATTE ≠ lattosio: helper) ── (Opzione C)
            if (tag == TagStandard.INT_LATTOSIO) {
                StatoAllergene latte = effettivo(stati, alimento, Allergene.LATTE);
                if (lattosioPresente(alimento, latte)) {
                    motivi.add(new MotivoValutazioneDto("INT_LATTOSIO",
                            "🟡 Contiene lattosio. Il paziente è intollerante al lattosio (Tag: INT_LATTOSIO)."));
                    if (ordine(LivelloAllerta.WARNING) > ordine(peggiore)) peggiore = LivelloAllerta.WARNING;
                } else if (latte == StatoAllergene.SCONOSCIUTO) {
                    motivi.add(new MotivoValutazioneDto("INT_LATTOSIO",
                            "ⓘ Presenza di lattosio non verificata per questo prodotto (Tag: INT_LATTOSIO)."));
                    if (ordine(LivelloAllerta.INFO) > ordine(peggiore)) peggiore = LivelloAllerta.INFO;
                }
                // ASSENTE / lactose-free / stagionato-burro → SAFE
            }
        }

        return new ValutazioneClinicaDto(peggiore, List.copyOf(motivi));
    }

    /**
     * Risoluzione Opzione C: stato effettivo di un allergene per un alimento.
     * Riga esplicita in {@code alimenti.allergeni} se presente; altrimenti il default
     * dell'alimento ({@code allergeni_default}): {@code ASSENTE→ASSENTE} (SAFE),
     * {@code SCONOSCIUTO} o default nullo {@code →SCONOSCIUTO} (INFO, "non verificato").
     * Il valore restituito non è mai {@code null}, così i rami chiamanti gestiscono i 4 stati.
     */
    private static StatoAllergene effettivo(Map<Allergene, StatoAllergene> stati,
                                            AlimentoBase alimento, Allergene allergene) {
        StatoAllergene s = stati.get(allergene);
        if (s != null) return s;
        DefaultAllergene def = alimento.getAllergeniDefault();
        return def == DefaultAllergene.ASSENTE ? StatoAllergene.ASSENTE : StatoAllergene.SCONOSCIUTO;
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
