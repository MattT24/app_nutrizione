package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaFormDto;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import it.nutrizionista.restnutrizionista.service.MisurazioneAntropometricaService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Guard del fix #3 (auto-sync fascicolo). Dopo una scrittura clinica (create misurazione),
 * {@code SchedaFascicoloSync} gira in {@code afterCommit} e DEVE persistere una riga
 * {@code documenti_fascicolo}. Prima del fix, {@code FascicoloService.sincronizzaDocumento} girava con
 * la propagazione di default (REQUIRED): invocato da {@code afterCommit} su una tx GIÀ committata,
 * l'INSERT partecipava alla connessione ormai chiusa e NON veniva committato (nessun errore; PDF orfano
 * su disco) → riga assente. Con {@code REQUIRES_NEW} la riga è committata.
 *
 * <p>⚠️ NON-{@code @Transactional} (via {@link SafeTestDatabaseBase}): la tx deve committare davvero,
 * altrimenti l'{@code afterCommit} non scatterebbe (falso verde). Il render è lo
 * {@code StubPdfRenderer} (profilo test → PDF valido, niente Chromium). A differenza di
 * {@code FascicoloAutoSyncBestEffortIntegrationTest} (che stubba il render a FALLIRE per testare il
 * best-effort), qui il render SUCCEDE e si asserisce la RIGA committata — esattamente il gap di
 * copertura che aveva lasciato passare #3 (il vecchio test verificava solo {@code verify(pdfRenderer)}).
 */
@SpringBootTest
@ActiveProfiles("test")
class FascicoloAutoSyncPersistsRowIntegrationTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri.fasc3@test.it";

    @Autowired private ClienteService clienteService;
    @Autowired private MisurazioneAntropometricaService misurazioneService;
    @Autowired private UtenteRepository utenteRepo;
    @Autowired private DocumentoFascicoloRepository fascicoloRepo;

    private Long clienteId;

    @BeforeEach
    void seed() {
        Utente u = new Utente();
        u.setNome("Nutri"); u.setCognome("Fasc3"); u.setCodiceFiscale("FSCNRZ00A00A000A");
        u.setEmail(EMAIL); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
        utenteRepo.save(u);
    }

    /** Il render (stub) scrive un PDF reale su disco: ripulisco la cartella del cliente creato. */
    @AfterEach
    void cleanupDisk() throws IOException {
        if (clienteId == null) return;
        Path dir = Paths.get("uploads/fascicoli", clienteId.toString());
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) { }
                });
            }
        }
    }

    private ClienteFormDto clienteForm() {
        ClienteFormDto f = new ClienteFormDto();
        f.setSesso(Sesso.Maschio); f.setNome("Mario"); f.setCognome("Rossi");
        f.setIntolleranze(""); f.setFunzioniIntestinali(""); f.setProblematicheSalutari("");
        f.setQuantitaEQualitaDelSonno(""); f.setAssunzioneFarmaci(""); f.setBeveAlcol(false); f.setFuma(false);
        return f;
    }

    @Test
    @WithMockUser(username = EMAIL)
    void create_misurazione_persisteRigaFascicolo_viaAfterCommit() {
        ClienteDto cliente = clienteService.create(clienteForm());
        clienteId = cliente.getId();

        MisurazioneAntropometricaFormDto form = new MisurazioneAntropometricaFormDto();
        ClienteDto ref = new ClienteDto();
        ref.setId(clienteId);
        form.setCliente(ref);
        form.setPeso(75.0);
        form.setDataMisurazione(LocalDate.now());
        MisurazioneAntropometricaDto mis = misurazioneService.create(form);

        // Fix #3: la riga documenti_fascicolo DEVE essere committata dall'afterCommit (REQUIRES_NEW).
        // Senza il fix (REQUIRED) l'INSERT non committa e questa asserzione fallisce.
        assertThat(fascicoloRepo.findByClienteIdAndTipoDocumentoAndRiferimentoId(
                clienteId, TipoDocumento.MISURAZIONE, mis.getId()))
                .as("il documento fascicolo MISURAZIONE deve essere persistito dopo la create (afterCommit)")
                .isPresent();
    }
}
