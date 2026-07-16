package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.dto.ApplicaSchedaTemplateRequest;
import it.nutrizionista.restnutrizionista.dto.CopyBulkRequest;
import it.nutrizionista.restnutrizionista.dto.CopyBulkResultDto;
import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateRequest;
import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateResultDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.PastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlimento;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoScheda;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.PastoTemplateApplyService;
import it.nutrizionista.restnutrizionista.service.SchedaService;
import it.nutrizionista.restnutrizionista.service.SchedaTemplateService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * E2E di sicurezza F-D1a (catena service completa, non solo unit): validano che gli alimenti
 * allergenici non entrino silenziosamente via template e che la ri-invocazione con inclusione
 * per-item sia idempotente (niente duplicati) + auditata.
 */
@SpringBootTest
@ActiveProfiles("test")
class FoodImportSafetyE2ETest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri.fd1a@test.it";

    @Autowired private PastoTemplateApplyService applyService;
    @Autowired private SchedaTemplateService schedaTemplateService;
    @Autowired private SchedaService schedaService;
    @Autowired private UtenteRepository utenteRepo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private SchedaRepository schedaRepo;
    @Autowired private PastoRepository pastoRepo;
    @Autowired private AlimentoBaseRepository alimentoRepo;
    @Autowired private PastoTemplateRepository pastoTemplateRepo;
    @Autowired private SchedaTemplateRepository schedaTemplateRepo;
    @Autowired private AuditLogRepository auditRepo;

    private Long pastoId;
    private Long schedaId;
    private Long clienteId;
    private Long alimentoAllergeneId;
    private Long alimentoSicuroId;
    private Long pastoTemplateId;

    @BeforeEach
    void seed() {
        Utente u = new Utente();
        u.setNome("Nutri"); u.setCognome("FD1a"); u.setCodiceFiscale("FDAAAA00A00A000A");
        u.setEmail(EMAIL); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
        Utente saved = utenteRepo.save(u);

        AlimentoBase allergene = alimentoRepo.save(aliment("Pane glutine", Map.of(Allergene.GLUTINE, StatoAllergene.PRESENTE)));
        AlimentoBase sicuro = alimentoRepo.save(aliment("Mela", null));
        alimentoAllergeneId = allergene.getId();
        alimentoSicuroId = sicuro.getId();

        Cliente c = cliente(saved);
        c.setTagStandard(Set.of(TagStandard.ALL_GLUTINE)); // allergico al glutine
        Cliente clienteSaved = clienteRepo.save(c);
        clienteId = clienteSaved.getId();

        Scheda s = new Scheda();
        s.setCliente(clienteSaved);
        s.setNome("Scheda FD1a");
        s.setAttiva(true);
        Scheda schedaSaved = schedaRepo.save(s);
        schedaId = schedaSaved.getId();

        Pasto p = new Pasto();
        p.setScheda(schedaSaved);
        p.setNome("Colazione");
        pastoId = pastoRepo.save(p).getId();

        // Pasto-template con l'allergene + un alimento sicuro.
        PastoTemplate t = new PastoTemplate();
        t.setNome("Template colazione");
        t.setCreatedBy(saved);
        t.getAlimenti().add(templateAlimento(t, allergene, 100.0));
        t.getAlimenti().add(templateAlimento(t, sicuro, 150.0));
        pastoTemplateId = pastoTemplateRepo.save(t).getId();
    }

    private static PastoTemplateAlimento templateAlimento(PastoTemplate t, AlimentoBase a, double qty) {
        PastoTemplateAlimento ta = new PastoTemplateAlimento();
        ta.setTemplate(t);
        ta.setAlimento(a);
        ta.setQuantita(qty);
        return ta;
    }

    private static AlimentoBase aliment(String nome, Map<Allergene, StatoAllergene> allergeni) {
        AlimentoBase a = new AlimentoBase();
        a.setNome(nome);
        a.setMisuraInGrammi(100.0);
        if (allergeni != null) a.setAllergeni(new EnumMap<>(allergeni));
        Macro m = new Macro();
        m.setAlimento(a);
        m.setCalorie(100.0); m.setProteine(10.0); m.setCarboidrati(10.0); m.setGrassi(10.0);
        a.setMacroNutrienti(m);
        return a;
    }

    private Cliente cliente(Utente nutrizionista) {
        Cliente c = new Cliente();
        c.setSesso(Sesso.Maschio);
        c.setNome("Mario"); c.setCognome("Rossi");
        c.setCodiceFiscale("MRARSS00A00A000A");
        c.setEmail("cliente.fd1a@test.it");
        c.setDataNascita(LocalDate.of(1990, 1, 1));
        c.setPeso(70.0); c.setAltezza(175);
        c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
        c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
        c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
        c.setBeveAlcol(false); c.setFuma(false);
        c.setNutrizionista(nutrizionista);
        return c;
    }

    private long countOverride() {
        return auditRepo.findAll().stream()
                .filter(a -> a.getAction() == AuditAction.OVERRIDE_ALERT_GRAVE)
                .count();
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "PASTO_UPDATE" })
    void applyTemplatePasto_skipAndReport_poiInclusionePerItem_idempotente() {
        // 1) Prima applicazione senza forzati: l'allergene è SALTATO, il sicuro inserito.
        PastoApplyTemplateRequest req1 = new PastoApplyTemplateRequest();
        req1.setTemplateId(pastoTemplateId);
        PastoApplyTemplateResultDto r1 = applyService.applyToPasto(pastoId, req1);

        assertThat(r1.getPasto().getAlimentiPasto()).as("solo il sicuro inserito").hasSize(1);
        assertThat(r1.getSkipped()).anySatisfy(s -> assertThat(s.getType()).isEqualTo("ALLERGENE_CLINICO"));
        assertThat(r1.getConflittiClinici())
                .anySatisfy(c -> {
                    assertThat(c.alimentoId()).isEqualTo(alimentoAllergeneId);
                    assertThat(c.allergeneDichiarato()).isTrue();
                });
        assertThat(countOverride()).as("nessun override finché non si include").isZero();

        // 2) Ri-applicazione con l'allergene incluso consapevolmente: inserito + audit, MA nessun duplicato del sicuro.
        PastoApplyTemplateRequest req2 = new PastoApplyTemplateRequest();
        req2.setTemplateId(pastoTemplateId);
        req2.setAlimentiForzatiIds(List.of(alimentoAllergeneId));
        PastoApplyTemplateResultDto r2 = applyService.applyToPasto(pastoId, req2);

        assertThat(r2.getPasto().getAlimentiPasto())
                .as("2 alimenti (sicuro + allergene forzato), NON 3 → idempotenza").hasSize(2);
        assertThat(countOverride()).as("una riga OVERRIDE per l'allergene incluso").isEqualTo(1);
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "SCHEDA_CREATE" })
    void creaSchedaDaTemplate_bloccaSenzaConferma_poiCreaConAuditSeForzato() {
        SchedaTemplate st = schedaTemplateConAllergene();

        // SchedaFormDto porta l'ENTITÀ Cliente (non un DTO): ricarico il cliente target.
        Cliente clienteTarget = clienteRepo.findById(clienteId).orElseThrow();
        SchedaFormDto form = new SchedaFormDto();
        form.setCliente(clienteTarget);
        form.setNome("Nuova da template");

        // Senza conferma → 409 e nessuna scheda creata.
        long schedeIniz = schedaRepo.count();
        assertThatThrownBy(() -> schedaTemplateService.creaSchedaDaTemplate(st.getId(), form, null, null))
                .isInstanceOf(ConflictException.class);
        assertThat(schedaRepo.count()).as("nessuna scheda creata sul blocco").isEqualTo(schedeIniz);

        // Con conferma + allergene forzato → scheda creata + audit.
        schedaTemplateService.creaSchedaDaTemplate(st.getId(), form, true, List.of(alimentoAllergeneId));
        assertThat(schedaRepo.count()).isEqualTo(schedeIniz + 1);
        assertThat(countOverride()).isEqualTo(1);
    }

    // #3 — applica scheda-template a scheda ESISTENTE (block-and-report 409 → override forzato + audit).
    @Test
    @WithMockUser(username = EMAIL, authorities = { "SCHEDA_UPDATE" })
    void applicaSchedaTemplate_bloccaSenzaConferma_poiApplicaConAuditSeForzato() {
        SchedaTemplate st = schedaTemplateConAllergene();

        ApplicaSchedaTemplateRequest req = new ApplicaSchedaTemplateRequest();
        req.setMode("REPLACE");

        // Senza conferma → 409 (gate clinico prima di applicare), niente override.
        assertThatThrownBy(() -> schedaTemplateService.applicaAScheda(st.getId(), schedaId, req))
                .isInstanceOf(ConflictException.class);
        assertThat(countOverride()).as("nessun override sul blocco").isZero();

        // Con conferma + allergene forzato → applicato + una riga OVERRIDE.
        req.setConfermaConflittiClinici(true);
        req.setAlimentiForzatiIds(List.of(alimentoAllergeneId));
        schedaTemplateService.applicaAScheda(st.getId(), schedaId, req);
        assertThat(countOverride()).as("override per l'applicazione forzata").isEqualTo(1);
    }

    // #4 — copy-bulk cross-paziente (skip-and-report 200 con report STRUTTURATO; force → copia + audit).
    @Test
    @WithMockUser(username = EMAIL, authorities = { "SCHEDA_CREATE" })
    void copyBulkCrossPaziente_reportStrutturato_poiForzaConAudit() {
        // Scheda sorgente (di clienteA) contenente l'allergene.
        Cliente clienteA = clienteRepo.findById(clienteId).orElseThrow();
        AlimentoBase allergene = alimentoRepo.findById(alimentoAllergeneId).orElseThrow();
        Scheda src = new Scheda();
        src.setCliente(clienteA); src.setNome("Sorgente copy"); src.setAttiva(true);
        Scheda srcSaved = schedaRepo.save(src);
        Pasto sp = new Pasto();
        sp.setScheda(srcSaved); sp.setNome("Colazione");
        sp.getAlimentiPasto().add(new AlimentoPasto(allergene, sp, 100));
        pastoRepo.save(sp);

        // Cliente B (diverso, anch'esso allergico al glutine) come target cross-paziente.
        Utente owner = utenteRepo.findByEmail(EMAIL).orElseThrow();
        Cliente cb = cliente(owner);
        cb.setCodiceFiscale("CLBBBB00B00B000B"); cb.setEmail("clienteB.fd1a@test.it");
        cb.setTagStandard(Set.of(TagStandard.ALL_GLUTINE));
        Long clienteBId = clienteRepo.save(cb).getId();

        // Senza force → item in conflitto con conflittiClinici STRUTTURATO (livello + allergeneDichiarato).
        CopyBulkRequest r = new CopyBulkRequest();
        r.setTargetClienteIds(List.of(clienteBId));
        r.setForce(false);
        CopyBulkResultDto res = schedaService.copyBulk(srcSaved.getId(), r);
        assertThat(res.getConflitti()).anySatisfy(item -> {
            assertThat(item.getClienteId()).isEqualTo(clienteBId);
            assertThat(item.getConflittiClinici()).anySatisfy(c -> {
                assertThat(c.alimentoId()).isEqualTo(alimentoAllergeneId);
                assertThat(c.allergeneDichiarato()).isTrue();
            });
        });
        assertThat(countOverride()).as("nessun override senza force").isZero();

        // Con force → copiato + audit override cross-paziente.
        CopyBulkRequest rf = new CopyBulkRequest();
        rf.setTargetClienteIds(List.of(clienteBId));
        rf.setForce(true);
        schedaService.copyBulk(srcSaved.getId(), rf);
        assertThat(countOverride()).as("override per la copia forzata cross-paziente").isEqualTo(1);
    }

    private SchedaTemplate schedaTemplateConAllergene() {
        Utente owner = utenteRepo.findByEmail(EMAIL).orElseThrow();
        AlimentoBase allergene = alimentoRepo.findById(alimentoAllergeneId).orElseThrow();
        SchedaTemplate st = new SchedaTemplate();
        st.setNome("Scheda-template FD1a");
        st.setCreatedBy(owner);
        st.setTipo(TipoScheda.GIORNALIERA);
        PastoSchedaTemplate pt = new PastoSchedaTemplate();
        pt.setNome("Colazione");
        pt.setSchedaTemplate(st);
        AlimentoPastoSchedaTemplate apt = new AlimentoPastoSchedaTemplate();
        apt.setPastoSchedaTemplate(pt);
        apt.setAlimento(allergene);
        apt.setQuantita(100);
        pt.getAlimenti().add(apt);
        st.getPasti().add(pt);
        return schedaTemplateRepo.save(st);
    }
}
