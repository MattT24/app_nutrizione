package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import it.nutrizionista.restnutrizionista.entity.AccettazioneDocumento;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TipoDocumento;
import it.nutrizionista.restnutrizionista.repository.AccettazioneDocumentoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.AccettazioneService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * A4/A9 — meccanismo di accettazione documenti alla registrazione: creazione delle 3 accettazioni,
 * validazione obbligatoria (400 se manca un'accettazione), versioning effettivo (pending al bump),
 * ri-accettazione e revoca, consultazione self.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccettazioneIntegrationTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "reg@test.it";

    @Autowired private MockMvc mvc;
    @Autowired private UtenteRepository repoUtente;
    @Autowired private RuoloRepository repoRuolo;
    @Autowired private AccettazioneDocumentoRepository repoAcc;
    @Autowired private AccettazioneService accettazioneService;

    @BeforeEach
    void seedRuolo() {
        // register() assegna il ruolo NUTRIZIONISTA → deve esistere (le tabelle sono truncate a ogni test).
        if (repoRuolo.findByAlias("NUTRIZIONISTA").isEmpty()) {
            Ruolo r = new Ruolo();
            r.setNome("Nutrizionista");
            r.setAlias("NUTRIZIONISTA");
            repoRuolo.save(r);
        }
    }

    @AfterEach
    void resetVersioni() {
        // Ripristina le versioni correnti (alcuni test le mutano via reflection per simulare il bump).
        ReflectionTestUtils.setField(accettazioneService, "versionePrivacy", "v1-draft");
        ReflectionTestUtils.setField(accettazioneService, "versioneTermini", "v1-draft");
        ReflectionTestUtils.setField(accettazioneService, "versioneDpa", "v1-draft");
    }

    private String body(boolean privacy, boolean termini, boolean dpa) {
        return ("{\"nome\":\"Mario\",\"cognome\":\"Rossi\",\"codiceFiscale\":\"RSSMRA80A01H501U\","
                + "\"email\":\"" + EMAIL + "\",\"password\":\"Password1\",\"telefono\":\"3330001122\","
                + "\"indirizzo\":\"Via Roma 1\",\"accettaPrivacy\":" + privacy
                + ",\"accettaTermini\":" + termini + ",\"accettaDpa\":" + dpa + "}");
    }

    private void register(boolean p, boolean t, boolean d) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(p, t, d)))
                .andExpect(status().isNoContent());
    }

    private Utente utenteRegistrato() {
        return repoUtente.findByEmail(EMAIL).orElseThrow();
    }

    @Test
    void register_conTutteLeAccettazioni_creaTreRighe() throws Exception {
        register(true, true, true);

        List<AccettazioneDocumento> acc = repoAcc.findByUtente_IdOrderByAccettatoAtDesc(utenteRegistrato().getId());
        assertEquals(3, acc.size());
        assertEquals(Set.of(TipoDocumento.PRIVACY_POLICY, TipoDocumento.TERMINI_SERVIZIO, TipoDocumento.DPA),
                acc.stream().map(AccettazioneDocumento::getTipo).collect(Collectors.toSet()));
        assertTrue(acc.stream().allMatch(a -> a.getRevocatoAt() == null), "nessuna revocata alla registrazione");
        assertTrue(acc.stream().allMatch(a -> "v1-draft".equals(a.getVersione())), "versione corrente");
    }

    @Test
    void register_senzaUnaAccettazione_400_eNessunUtenteCreato() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(true, true, false)))
                .andExpect(status().isBadRequest());
        assertTrue(repoUtente.findByEmail(EMAIL).isEmpty(), "utente NON creato se manca un'accettazione");
    }

    @Test
    void pending_vuotoDopoRegistrazione_poiNonVuotoAlBumpVersione() throws Exception {
        register(true, true, true);
        Utente u = utenteRegistrato();
        assertTrue(accettazioneService.documentiDaAccettare(u).isEmpty(), "nessun pending subito dopo la registrazione");

        // Simula il bump della versione Privacy (in Wave 3 arriva il testo nuovo).
        ReflectionTestUtils.setField(accettazioneService, "versionePrivacy", "v2");
        assertEquals(List.of(TipoDocumento.PRIVACY_POLICY), accettazioneService.documentiDaAccettare(u),
                "al cambio versione, Privacy torna tra i pending → versioning effettivo");

        // Ri-accettazione → pending svuotato.
        accettazioneService.accetta(u, Set.of(TipoDocumento.PRIVACY_POLICY));
        assertTrue(accettazioneService.documentiDaAccettare(u).isEmpty());
    }

    @Test
    void revoca_riportaIlDocumentoTraIPending() throws Exception {
        register(true, true, true);
        Utente u = utenteRegistrato();

        accettazioneService.revoca(u, TipoDocumento.TERMINI_SERVIZIO);

        assertTrue(accettazioneService.documentiDaAccettare(u).contains(TipoDocumento.TERMINI_SERVIZIO),
                "dopo la revoca non c'è più un'accettazione attiva → il documento è pending");
    }

    @Test
    @WithMockUser(username = EMAIL)
    void getMe_ritornaLeAccettazioni() throws Exception {
        register(true, true, true);
        mvc.perform(get("/api/accettazioni/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
