package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoRequest;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.Allergene;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.StatoAllergene;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.exception.PdfGenerationException;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.AlimentoPastoService;
import it.nutrizionista.restnutrizionista.service.PdfRenderer;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Guard di regressione per PDF-01: la sincronizzazione automatica del fascicolo è <b>best-effort</b>
 * e NON deve mai far fallire la scrittura clinica che l'ha innescata, nemmeno quando il rendering PDF
 * fallisce. Prima del fix il render girava DENTRO la transazione di scrittura: una
 * {@link PdfGenerationException} marcava la tx rollback-only e — nonostante il {@code catch}
 * "best-effort" — al commit lanciava {@code UnexpectedRollbackException}, annullando sia l'inserimento
 * dell'alimento sia la riga di audit {@code OVERRIDE_ALERT_GRAVE} (perdita silenziosa di dato clinico
 * + traccia di sicurezza). Col fix la sync è registrata su {@code afterCommit}, quindi gira DOPO il
 * commit in una tx propria: il fallimento del render è isolato.
 *
 * <p>⚠️ Questo test <b>non è {@code @Transactional}</b>: la transazione deve committare davvero,
 * altrimenti l'hook {@code afterCommit} non scatterebbe mai (falso verde). {@link SafeTestDatabaseBase}
 * ripulisce le tabelle prima di ogni test, quindi i dati committati non "sporcano" gli altri.
 *
 * <p>Il {@link PdfRenderer} è mockato per lanciare sempre: nessun Chromium coinvolto. Con
 * {@code clinica.engine.use-allergen-rule=true} l'inserimento di un alimento con allergene GLUTINE
 * dichiarato in un cliente con tag {@code ALL_GLUTINE} è un {@code ALERT_GRAVE}, superato con
 * {@code confermaBloccoGrave=true} → produce la riga di audit {@code OVERRIDE_ALERT_GRAVE} nella
 * stessa tx della scrittura, che è esattamente ciò che il vecchio bug faceva sparire.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "clinica.engine.use-allergen-rule=true")
class FascicoloAutoSyncBestEffortIntegrationTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri-bestfeffort@test.it";

    @MockBean private PdfRenderer pdfRenderer; // sostituisce lo stub: qui il render DEVE fallire

    @Autowired private AlimentoPastoService alimentoPastoService;
    @Autowired private UtenteRepository repoUtente;
    @Autowired private ClienteRepository repoCliente;
    @Autowired private SchedaRepository repoScheda;
    @Autowired private PastoRepository repoPasto;
    @Autowired private AlimentoBaseRepository repoAlimento;
    @Autowired private AlimentoPastoRepository repoAlimentoPasto;
    @Autowired private AuditLogRepository repoAudit;

    private Long clienteId;
    private Long pastoId;
    private Long alimentoId;
    private Long schedaId;

    @BeforeEach
    void seed() {
        when(pdfRenderer.render(anyString()))
                .thenThrow(new PdfGenerationException("Render PDF forzato a fallire (test PDF-01)"));

        Utente nut = new Utente();
        nut.setNome("Best"); nut.setCognome("Effort");
        nut.setCodiceFiscale("BSTFRT00A00A000A");
        nut.setEmail(EMAIL); nut.setPassword("x"); nut.setTelefono("000"); nut.setIndirizzo("x");
        nut = repoUtente.save(nut);

        Cliente c = new Cliente();
        c.setSesso(Sesso.Maschio);
        c.setNome("Celiaco"); c.setCognome("Test");
        c.setCodiceFiscale("CLCTST00A00A000A");
        c.setDataNascita(LocalDate.of(1990, 1, 1));
        c.setPeso(80.0); c.setAltezza(180);
        c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
        c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
        c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
        c.setBeveAlcol(false); c.setFuma(false);
        c.setNutrizionista(nut);
        c.setTagStandard(Set.of(TagStandard.ALL_GLUTINE)); // allergia dichiarata → blocco GRAVE
        clienteId = repoCliente.save(c).getId();

        Scheda s = new Scheda();
        s.setCliente(c); s.setNome("Scheda PDF-01"); s.setAttiva(true);
        Scheda scheda = repoScheda.save(s);
        schedaId = scheda.getId();

        Pasto pasto = new Pasto();
        pasto.setScheda(scheda); pasto.setNome("Pranzo");
        pastoId = repoPasto.save(pasto).getId();

        AlimentoBase alimento = new AlimentoBase();
        alimento.setNome("Pane"); alimento.setMisuraInGrammi(100.0);
        Map<Allergene, StatoAllergene> allergeni = new EnumMap<>(Allergene.class);
        allergeni.put(Allergene.GLUTINE, StatoAllergene.PRESENTE);
        alimento.setAllergeni(allergeni);
        Macro m = new Macro();
        m.setAlimento(alimento);
        m.setCalorie(250.0); m.setProteine(9.0); m.setCarboidrati(50.0); m.setGrassi(2.0);
        alimento.setMacroNutrienti(m);
        alimentoId = repoAlimento.save(alimento).getId();
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "SCHEDA_UPDATE" })
    void renderPdfFallito_nonAnnullaLaScritturaClinicaNeLAudit() {
        AlimentoPastoRequest req = new AlimentoPastoRequest();
        req.setPasto(new IdRequest(pastoId));
        req.setAlimento(new IdRequest(alimentoId));
        req.setQuantita(100);
        req.setConfermaBloccoGrave(true); // supero consapevolmente il blocco ALERT_GRAVE

        // Non deve lanciare: il fallimento del render (afterCommit) è inghiottito best-effort.
        alimentoPastoService.associaAlimento(req);

        // 1) La scrittura clinica è COMMITTATA nonostante il render fallito.
        assertThat(repoAlimentoPasto.existsByPasto_IdAndAlimento_Id(pastoId, alimentoId))
                .as("l'alimento deve restare associato al pasto anche se la sync PDF fallisce")
                .isTrue();

        // 2) La riga di audit dell'override GRAVE è COMMITTATA (era ciò che il rollback faceva sparire).
        assertThat(repoAudit.search(clienteId, null, AuditAction.OVERRIDE_ALERT_GRAVE, null, null,
                PageRequest.of(0, 10)).getTotalElements())
                .as("la riga OVERRIDE_ALERT_GRAVE deve sopravvivere al fallimento della sync PDF")
                .isGreaterThan(0);

        // 3) Il render è stato davvero tentato (dopo il commit) e ha fallito: prova che il path afterCommit gira.
        verify(pdfRenderer, atLeastOnce()).render(anyString());
    }
}
