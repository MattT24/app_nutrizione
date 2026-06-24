package it.nutrizionista.restnutrizionista.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;

/**
 * Unit test della regola clinica data-driven {@link AllergeneRule} (matrice tag×stato, piano §3.1 / E.13).
 * Pura logica: nessun contesto Spring/DB.
 */
class AllergeneRuleTest {

    private final AllergeneRule rule = new AllergeneRule();

    private static AlimentoBase aliment(String nome, Map<Allergene, StatoAllergene> allergeni) {
        AlimentoBase a = new AlimentoBase();
        a.setNome(nome);
        EnumMap<Allergene, StatoAllergene> m = new EnumMap<>(Allergene.class);
        if (allergeni != null) m.putAll(allergeni);
        a.setAllergeni(m);
        return a;
    }

    private ValutazioneClinicaDto valuta(AlimentoBase a, TagStandard... tags) {
        return rule.valida(a, Set.of(tags), Set.of());
    }

    // ── Allergie ALL_* ──

    @Test
    void allergiaPresente_daAlertGrave() {
        AlimentoBase a = aliment("Pane", Map.of(Allergene.GLUTINE, StatoAllergene.PRESENTE));
        ValutazioneClinicaDto r = valuta(a, TagStandard.ALL_GLUTINE);
        assertThat(r.stato()).isEqualTo(LivelloAllerta.ALERT_GRAVE);
        assertThat(r.motivi()).hasSize(1);
        assertThat(r.motivi().get(0).codiceTrigger()).isEqualTo("ALL_GLUTINE");
    }

    @Test
    void allergiaTracce_daWarning() {
        AlimentoBase a = aliment("Cioccolato", Map.of(Allergene.FRUTTA_GUSCIO, StatoAllergene.TRACCE));
        assertThat(valuta(a, TagStandard.ALL_FRUTTA_GUSCIO).stato()).isEqualTo(LivelloAllerta.WARNING);
    }

    @Test
    void allergiaAssente_daSafe() {
        AlimentoBase a = aliment("Riso", Map.of(Allergene.GLUTINE, StatoAllergene.ASSENTE));
        ValutazioneClinicaDto r = valuta(a, TagStandard.ALL_GLUTINE);
        assertThat(r.stato()).isEqualTo(LivelloAllerta.SAFE);
        assertThat(r.motivi()).isEmpty();
    }

    @Test
    void allergiaSconosciuta_daInfo() {
        AlimentoBase a = aliment("Prodotto ignoto", Map.of()); // nessuna entry = SCONOSCIUTO
        ValutazioneClinicaDto r = valuta(a, TagStandard.ALL_SOIA);
        assertThat(r.stato()).isEqualTo(LivelloAllerta.INFO);
        assertThat(r.motivi()).hasSize(1);
    }

    @Test
    void copreNuoviAllergeniUe() {
        // Es. Nutella: latte+frutta a guscio+soia presenti → paziente allergico a frutta a guscio protetto
        AlimentoBase nutella = aliment("Nutella", Map.of(
                Allergene.LATTE, StatoAllergene.PRESENTE,
                Allergene.FRUTTA_GUSCIO, StatoAllergene.PRESENTE,
                Allergene.SOIA, StatoAllergene.PRESENTE));
        assertThat(valuta(nutella, TagStandard.ALL_FRUTTA_GUSCIO).stato()).isEqualTo(LivelloAllerta.ALERT_GRAVE);
        assertThat(valuta(nutella, TagStandard.ALL_MOLLUSCHI).stato()).isEqualTo(LivelloAllerta.INFO); // molluschi sconosciuti
    }

    // ── Intolleranza NCGS (glutine) ──

    @Test
    void ncgsGlutinePresente_daWarning() {
        AlimentoBase a = aliment("Pane", Map.of(Allergene.GLUTINE, StatoAllergene.PRESENTE));
        assertThat(valuta(a, TagStandard.INT_GLUTINE_NCGS).stato()).isEqualTo(LivelloAllerta.WARNING);
    }

    @Test
    void ncgsSconosciuto_daInfo() {
        AlimentoBase a = aliment("Prodotto ignoto", Map.of());
        assertThat(valuta(a, TagStandard.INT_GLUTINE_NCGS).stato()).isEqualTo(LivelloAllerta.INFO);
    }

    // ── Intolleranza al lattosio (helper: LATTE ≠ lattosio) ──

    @Test
    void lattosioLattePresente_daWarning() {
        AlimentoBase a = aliment("Latte intero", Map.of(Allergene.LATTE, StatoAllergene.PRESENTE));
        assertThat(valuta(a, TagStandard.INT_LATTOSIO).stato()).isEqualTo(LivelloAllerta.WARNING);
    }

    @Test
    void lattosioLactoseFree_daSafe() {
        AlimentoBase a = aliment("Latte senza lattosio", Map.of(Allergene.LATTE, StatoAllergene.PRESENTE));
        a.setSenzaLattosio(true);
        assertThat(valuta(a, TagStandard.INT_LATTOSIO).stato()).isEqualTo(LivelloAllerta.SAFE);
    }

    @Test
    void lattosioFormaggioStagionato_daSafe() {
        // Stagionato/burro: LATTE presente ma lattosio ~0 (keyword low-lactose, E.9)
        AlimentoBase a = aliment("Parmigiano Reggiano stagionato 24 mesi", Map.of(Allergene.LATTE, StatoAllergene.PRESENTE));
        assertThat(valuta(a, TagStandard.INT_LATTOSIO).stato()).isEqualTo(LivelloAllerta.SAFE);
    }

    @Test
    void lattosioSconosciuto_daInfo() {
        AlimentoBase a = aliment("Prodotto ignoto", Map.of());
        assertThat(valuta(a, TagStandard.INT_LATTOSIO).stato()).isEqualTo(LivelloAllerta.INFO);
    }

    // ── Aggregazione e casi limite ──

    @Test
    void aggregaIlLivelloPeggiore() {
        AlimentoBase a = aliment("Misto", Map.of(Allergene.GLUTINE, StatoAllergene.PRESENTE)); // LATTE sconosciuto
        ValutazioneClinicaDto r = valuta(a, TagStandard.ALL_GLUTINE, TagStandard.ALL_LATTE);
        assertThat(r.stato()).isEqualTo(LivelloAllerta.ALERT_GRAVE); // GRAVE (glutine) prevale su INFO (latte)
        assertThat(r.motivi()).hasSize(2);
    }

    @Test
    void nessunTag_daSafe() {
        AlimentoBase a = aliment("Acqua", Map.of());
        assertThat(rule.valida(a, Set.of(), Set.of()).stato()).isEqualTo(LivelloAllerta.SAFE);
    }

    @Test
    void tagNonAllergene_ignorato() {
        AlimentoBase a = aliment("Salame", Map.of(Allergene.GLUTINE, StatoAllergene.PRESENTE));
        // PAT_IPERTENSIONE non è gestito da AllergeneRule → SAFE
        assertThat(valuta(a, TagStandard.PAT_IPERTENSIONE).stato()).isEqualTo(LivelloAllerta.SAFE);
    }
}
