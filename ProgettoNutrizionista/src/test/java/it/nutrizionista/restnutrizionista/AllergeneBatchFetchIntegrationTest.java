package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.service.ClinicalEngineService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Guard d'integrazione dell'invariante fetch di PR-1: {@code ClinicalEngineService.valutaInBatch}
 * pre-inizializza {@code allergeni} sul thread della transazione PRIMA del {@code parallelStream}.
 *
 * <p>Esercita il path reale del batch: gli alimenti sono ricaricati dal DB via
 * {@code findAllByIdInWithMacro} (fetch di macro/tracce ma NON di allergeni) dopo {@code em.clear()},
 * quindi {@code allergeni} è un proxy LAZY. {@code valutaInBatch} li valuta in {@code parallelStream}
 * su un dataset ampio (massimizza il fork). Se la pre-init venisse rimossa, i worker del ForkJoinPool
 * accederebbero alla LAZY fuori dalla Session (open-in-view=false) → {@code LazyInitializationException}
 * o masking → il target non risulterebbe ALERT_GRAVE.
 *
 * <p>Chiama {@code valutaInBatch} direttamente con un {@link Cliente} <strong>in-RAM</strong> (nessun
 * persist di Utente/Cliente → nessun side-effect, es. eventi gamification, che inquinerebbe il DB H2
 * condiviso tra @SpringBootTest). {@code @Transactional} → rollback degli alimenti creati.
 *
 * <p>Nota onesta: il fork del parallelStream non è deterministico; il valore del test è la
 * <strong>regressione anti-masking</strong> sul data-flow (allergeni caricata → nessun masking).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AllergeneBatchFetchIntegrationTest extends SafeTestDatabaseBase {

    @Autowired private ClinicalEngineService clinicalEngineService;
    @Autowired private AlimentoBaseRepository alimentoBaseRepository;
    @PersistenceContext private EntityManager em;

    private List<Long> ids;
    private Long targetId;

    @BeforeEach
    void setup() {
        List<AlimentoBase> alimenti = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            alimenti.add(alimentoConMacro("Filler " + i, null));
        }
        Map<Allergene, StatoAllergene> allergeniTarget = new EnumMap<>(Allergene.class);
        allergeniTarget.put(Allergene.FRUTTA_GUSCIO, StatoAllergene.PRESENTE);
        alimenti.add(alimentoConMacro("TARGET (FRUTTA_GUSCIO PRESENTE)", allergeniTarget));

        List<AlimentoBase> saved = alimentoBaseRepository.saveAll(alimenti);
        ids = saved.stream().map(AlimentoBase::getId).toList();
        targetId = ids.get(ids.size() - 1);

        // Svuota il persistence-context: gli alimenti verranno RICARICATI con 'allergeni' come proxy
        // LAZY (senza clear resterebbero managed con la mappa già inizializzata → il guard non testerebbe
        // il path lazy/parallelStream).
        em.flush();
        em.clear();
    }

    private static AlimentoBase alimentoConMacro(String nome, Map<Allergene, StatoAllergene> allergeni) {
        AlimentoBase a = new AlimentoBase();
        a.setNome(nome);
        a.setMisuraInGrammi(100.0);
        if (allergeni != null) {
            a.setAllergeni(allergeni);
        }
        Macro m = new Macro();
        m.setAlimento(a);
        m.setCalorie(100.0);
        m.setProteine(10.0);
        m.setCarboidrati(10.0);
        m.setGrassi(10.0);
        a.setMacroNutrienti(m);
        return a;
    }

    @Test
    void valutaInBatch_targetAlertGrave_conAllergeniLazyPreCaricati() {
        // Ricarica dal DB: macro/tracce fetchate, allergeni LAZY (come nel path reale di listIndex).
        List<AlimentoBase> alimenti = alimentoBaseRepository.findAllByIdInWithMacro(ids);
        assertThat(alimenti).hasSize(ids.size());

        // Cliente in-RAM (non persistito): tag ALL_FRUTTA_GUSCIO, nessun id → nessuna avversione.
        Cliente cliente = new Cliente();
        cliente.setTagStandard(Set.of(TagStandard.ALL_FRUTTA_GUSCIO));

        List<ValutazioneClinicaDto> valutazioni = clinicalEngineService.valutaInBatch(alimenti, cliente);

        int idxTarget = -1;
        for (int i = 0; i < alimenti.size(); i++) {
            if (targetId.equals(alimenti.get(i).getId())) { idxTarget = i; break; }
        }
        assertThat(idxTarget).as("target presente nel batch").isGreaterThanOrEqualTo(0);

        // Anti-masking: FRUTTA_GUSCIO=PRESENTE + tag paziente → ALERT_GRAVE, non mascherato a default/INFO.
        // Senza la pre-init di allergeni in valutaInBatch: LazyInitializationException o masking nei worker.
        assertThat(valutazioni.get(idxTarget).stato()).isEqualTo(LivelloAllerta.ALERT_GRAVE);
    }
}
