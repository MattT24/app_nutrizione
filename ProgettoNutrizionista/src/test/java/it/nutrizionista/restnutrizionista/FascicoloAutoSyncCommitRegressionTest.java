package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaFormDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaDto;
import it.nutrizionista.restnutrizionista.dto.PastoFormDto;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.MisurazioneAntropometricaService;
import it.nutrizionista.restnutrizionista.service.PastoService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Guard di regressione: la sincronizzazione automatica del fascicolo (vedi {@code SchedaFascicoloSync})
 * deve produrre davvero un {@code DocumentoFascicolo} committato, non solo evitare eccezioni.
 *
 * <p>Bug scoperto durante il testing manuale (segnalato dall'utente: "la creazione del pdf non manda
 * più il pdf nel fascicolo, in qualsiasi misurazioni/plico/schede"): {@code SchedaFascicoloSync}
 * registra la sincronizzazione su {@code TransactionSynchronization#afterCommit()} per isolarla dalla
 * tx di dominio (PDF-01). Il problema è che {@code afterCommit()} gira MENTRE la tx di dominio sta
 * ancora completando (Spring pulisce la sincronizzazione DOPO aver invocato afterCommit) — quindi
 * {@code FascicoloService.sincronizzaDocumento}, con la propagazione di default (REQUIRED), "partecipava"
 * a quella transazione-in-chiusura invece di aprirne una genuinamente nuova. Nessuna eccezione veniva
 * sollevata (il {@code catch} best-effort di {@code SchedaFascicoloSync} restava silenzioso), ma il
 * salvataggio non veniva mai davvero committato: il documento fascicolo spariva silenziosamente.
 * Fix: {@code @Transactional(propagation = Propagation.REQUIRES_NEW)} su {@code sincronizzaDocumento}
 * (stesso pattern già usato da {@code AuditService.recordCriticalNewTx} per lo stesso identico motivo).
 *
 * <p>Prima del fix questi test fallivano con {@code Optional} vuoto (nessuna eccezione, nessun log —
 * per questo era invisibile). {@link FascicoloAutoSyncBestEffortIntegrationTest} non lo copriva: testa
 * solo il path di FALLIMENTO del render (renderer mockato per lanciare sempre), quindi il rendering non
 * arriva mai a {@code fascicoloRepository.save(doc)} e non poteva rivelare questo bug sul commit.
 *
 * <p>NON {@code @Transactional}: serve che la tx di dominio committi davvero perché l'hook
 * {@code afterCommit} scatti (stesso motivo di {@link FascicoloAutoSyncBestEffortIntegrationTest}).
 * L'hook gira in modo SINCRONO al termine della tx del chiamante, quindi si può asserire subito dopo
 * la chiamata al service, senza attese/polling.
 */
@SpringBootTest
@ActiveProfiles("test")
class FascicoloAutoSyncCommitRegressionTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri-fascicolo-commit@test.it";

    @Autowired private PastoService pastoService;
    @Autowired private MisurazioneAntropometricaService misurazioneService;
    @Autowired private UtenteRepository repoUtente;
    @Autowired private ClienteRepository repoCliente;
    @Autowired private SchedaRepository repoScheda;
    @Autowired private DocumentoFascicoloRepository repoFascicolo;

    private Long clienteId;
    private Long schedaId;

    @BeforeEach
    void seed() {
        Utente nut = new Utente();
        nut.setNome("Fascicolo"); nut.setCognome("Commit");
        nut.setCodiceFiscale("FSCCMT00A00A000A");
        nut.setEmail(EMAIL); nut.setPassword("x"); nut.setTelefono("000"); nut.setIndirizzo("x");
        Utente nutSalvato = repoUtente.save(nut);

        Cliente c = new Cliente();
        c.setSesso(Sesso.Maschio);
        c.setNome("Test"); c.setCognome("Fascicolo");
        c.setCodiceFiscale("TSTFSC00A00A000A");
        c.setDataNascita(LocalDate.of(1990, 1, 1));
        c.setPeso(80.0); c.setAltezza(180);
        c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
        c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
        c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
        c.setBeveAlcol(false); c.setFuma(false);
        c.setNutrizionista(nutSalvato);
        clienteId = repoCliente.save(c).getId();

        Scheda s = new Scheda();
        s.setCliente(c); s.setNome("Scheda test fascicolo"); s.setAttiva(true);
        schedaId = repoScheda.save(s).getId();
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "MEAL_CREATE" })
    void creazionePasto_generaDocumentoFascicoloScheda() {
        PastoFormDto form = new PastoFormDto();
        SchedaDto schedaRef = new SchedaDto();
        schedaRef.setId(schedaId);
        form.setScheda(schedaRef);
        form.setNome("Pranzo");
        pastoService.create(form);

        assertThat(repoFascicolo.findByClienteIdAndTipoDocumentoAndRiferimentoId(clienteId, TipoDocumento.SCHEDA, schedaId))
                .as("dopo la creazione di un pasto ci si aspetta un DocumentoFascicolo SCHEDA auto-generato e committato")
                .isPresent();
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "MISURAZIONE_ANTROPOMETRICA_CREATE" })
    void creazioneMisurazione_generaDocumentoFascicoloMisurazione() {
        MisurazioneAntropometricaFormDto form = new MisurazioneAntropometricaFormDto();
        ClienteDto clienteDto = new ClienteDto();
        clienteDto.setId(clienteId);
        form.setCliente(clienteDto);
        form.setDataMisurazione(LocalDate.now());
        form.setPeso(80.0);
        MisurazioneAntropometricaDto salvata = misurazioneService.create(form);

        assertThat(repoFascicolo.findByClienteIdAndTipoDocumentoAndRiferimentoId(clienteId, TipoDocumento.MISURAZIONE, salvata.getId()))
                .as("dopo la creazione di una misurazione ci si aspetta un DocumentoFascicolo MISURAZIONE auto-generato e committato")
                .isPresent();
    }
}
