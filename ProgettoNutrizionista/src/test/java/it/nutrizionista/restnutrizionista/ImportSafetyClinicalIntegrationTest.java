package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ConflittoClinicoDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.engine.TagStandardAllergeneMapping;
import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.service.ClinicalEngineService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Integrazione F-D1a: {@link ClinicalEngineService#conflittiClinici} col motore REALE (AllergeneRule),
 * a garanzia che i {@code codiceTrigger} effettivamente emessi dalle regole allergene siano riconosciuti
 * come {@code allergeneDichiarato} (nota di review #2). Cliente in-RAM (nessun persist di Utente/Cliente),
 * alimenti persistiti; {@code @Transactional} → rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportSafetyClinicalIntegrationTest extends SafeTestDatabaseBase {

    @Autowired private ClinicalEngineService clinicalEngineService;
    @Autowired private AlimentoBaseRepository alimentoBaseRepository;

    private static AlimentoBase aliment(String nome, Map<Allergene, StatoAllergene> allergeni) {
        AlimentoBase a = new AlimentoBase();
        a.setNome(nome);
        a.setMisuraInGrammi(100.0);
        if (allergeni != null) a.setAllergeni(allergeni);
        Macro m = new Macro();
        m.setAlimento(a);
        m.setCalorie(100.0);
        m.setProteine(10.0);
        m.setCarboidrati(10.0);
        m.setGrassi(10.0);
        a.setMacroNutrienti(m);
        return a;
    }

    private static Map<Allergene, StatoAllergene> mappa(Allergene all, StatoAllergene stato) {
        Map<Allergene, StatoAllergene> m = new EnumMap<>(Allergene.class);
        m.put(all, stato);
        return m;
    }

    @Test
    void allergenePresente_conTagPaziente_graveEAllergeneDichiarato() {
        AlimentoBase pane = alimentoBaseRepository.save(aliment("Pane", mappa(Allergene.GLUTINE, StatoAllergene.PRESENTE)));
        Cliente cliente = new Cliente();
        cliente.setTagStandard(Set.of(TagStandard.ALL_GLUTINE));

        List<ConflittoClinicoDto> out = clinicalEngineService.conflittiClinici(cliente, List.of(pane));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).livello()).isEqualTo(LivelloAllerta.ALERT_GRAVE);
        assertThat(out.get(0).allergeneDichiarato()).isTrue();
        assertThat(out.get(0).nome()).isEqualTo("Pane");
    }

    @Test
    void tuttiI14Allergeni_riconosciuti_comeAllergeneDichiarato() {
        // Un alimento per allergene (PRESENTE) + un cliente con TUTTI i tag ALL_*: ogni alimento
        // deve risultare ALERT_GRAVE con allergeneDichiarato=true → prova che i codici reali della
        // regola coprono l'intera mappa TagStandardAllergeneMapping.
        List<AlimentoBase> alimenti = new ArrayList<>();
        Set<TagStandard> tagPaziente = new java.util.HashSet<>();
        for (TagStandard tag : TagStandard.values()) {
            Allergene all = TagStandardAllergeneMapping.allergeneFor(tag);
            if (all == null) continue;
            alimenti.add(aliment("Cibo_" + all.name(), mappa(all, StatoAllergene.PRESENTE)));
            tagPaziente.add(tag);
        }
        List<AlimentoBase> saved = alimentoBaseRepository.saveAll(alimenti);

        Cliente cliente = new Cliente();
        cliente.setTagStandard(tagPaziente);

        List<ConflittoClinicoDto> out = clinicalEngineService.conflittiClinici(cliente, saved);

        assertThat(out).as("un conflitto grave per ciascun allergene").hasSize(saved.size());
        assertThat(out).allSatisfy(c -> {
            assertThat(c.livello()).isEqualTo(LivelloAllerta.ALERT_GRAVE);
            assertThat(c.allergeneDichiarato())
                    .as("allergeneDichiarato per %s", c.nome())
                    .isTrue();
        });
    }

    @Test
    void allergeneTracce_conTagPaziente_warning() {
        AlimentoBase a = alimentoBaseRepository.save(aliment("Biscotto", mappa(Allergene.LATTE, StatoAllergene.TRACCE)));
        Cliente cliente = new Cliente();
        cliente.setTagStandard(Set.of(TagStandard.ALL_LATTE));

        List<ConflittoClinicoDto> out = clinicalEngineService.conflittiClinici(cliente, List.of(a));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).livello()).isEqualTo(LivelloAllerta.WARNING);
    }

    @Test
    void alimentoSicuro_nessunConflitto() {
        AlimentoBase a = alimentoBaseRepository.save(aliment("Mela", null));
        Cliente cliente = new Cliente();
        cliente.setTagStandard(Set.of(TagStandard.ALL_GLUTINE));

        List<ConflittoClinicoDto> out = clinicalEngineService.conflittiClinici(cliente, List.of(a));

        assertThat(out).isEmpty();
    }
}
