package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.AccettazioneDocumentoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.security.KeycloakAdminClient;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Onboarding admin-invite (beta Keycloak): gate SUPER_ADMIN sull'endpoint + creazione Utente (ruolo NUTRIZIONISTA,
 * subjectId NULL, ZERO accettazioni — catturate al 1° login/Wave-3) + invito Keycloak. {@code KeycloakAdminClient}
 * mockato via {@code @MockitoBean} → bean presente → l'{@code ObjectProvider} del service lo trova anche in profilo
 * test-legacy (dove {@code KeycloakAdminConfig} gated non lo crea).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminNutrizionistaControllerTest extends SafeTestDatabaseBase {

    @Autowired private MockMvc mvc;
    @Autowired private UtenteRepository repoUtente;
    @Autowired private RuoloRepository repoRuolo;
    @Autowired private AccettazioneDocumentoRepository repoAccettazione;
    @MockitoBean private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void seedRuoli() {
        Ruolo n = new Ruolo();
        n.setNome("NUTRIZIONISTA");
        n.setAlias("NUTRIZIONISTA");
        repoRuolo.save(n);
        Ruolo sa = new Ruolo();
        sa.setNome("SUPER_ADMIN");
        sa.setAlias("SUPER_ADMIN");
        repoRuolo.save(sa);
    }

    private Utente seedUtente(String email, String cf, String ruoloAlias, String subjectId) {
        Ruolo ruolo = repoRuolo.findByAlias(ruoloAlias).orElseThrow();
        Utente u = new Utente();
        u.setNome("N"); u.setCognome("N"); u.setCodiceFiscale(cf);
        u.setEmail(email); u.setPassword("x"); u.setTelefono("-"); u.setIndirizzo("-");
        u.setRuolo(ruolo); u.setSubjectId(subjectId);
        return repoUtente.save(u);
    }

    @Test
    @WithMockUser(username = "sa@test.it", authorities = {})
    void invita_senzaSuperAdmin_403() throws Exception {
        mvc.perform(post("/api/admin/nutrizionisti").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Anna\",\"cognome\":\"Rossi\",\"email\":\"anna@studio.it\"}"))
           .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "sa@test.it", authorities = { "SUPER_ADMIN" })
    void invita_conSuperAdmin_creaUtenteEInvito() throws Exception {
        mvc.perform(post("/api/admin/nutrizionisti").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Anna\",\"cognome\":\"Rossi\",\"email\":\"anna@studio.it\"}"))
           .andExpect(status().isCreated());

        Utente u = repoUtente.findWithAuthoritiesByEmail("anna@studio.it").orElse(null);
        assertNotNull(u, "l'Utente nutrizionista deve essere creato");
        assertEquals("NUTRIZIONISTA", u.getRuolo().getAlias(), "ruolo NUTRIZIONISTA");
        assertNull(u.getSubjectId(), "subjectId NULL finché non fa il 1° login OIDC");
        assertTrue(repoAccettazione.findByUtente_IdOrderByAccettatoAtDesc(u.getId()).isEmpty(),
                "NESSUNA accettazione all'onboarding (catturate al 1° login, Wave-3)");
        verify(keycloakAdminClient).createUser("anna@studio.it", "Anna", "Rossi");
    }

    @Test
    @WithMockUser(username = "sa@test.it", authorities = { "SUPER_ADMIN" })
    void invita_emailGiaAttiva_409() throws Exception {
        Ruolo ruolo = repoRuolo.findByAlias("NUTRIZIONISTA").orElseThrow();
        Utente attivo = new Utente();
        attivo.setNome("Gia"); attivo.setCognome("Attivo"); attivo.setCodiceFiscale("ATTIVO00A00A000A");
        attivo.setEmail("gia@studio.it"); attivo.setPassword("x"); attivo.setTelefono("-"); attivo.setIndirizzo("-");
        attivo.setRuolo(ruolo); attivo.setSubjectId("sub-esistente"); // già loggato via IdP → attivo
        repoUtente.save(attivo);

        mvc.perform(post("/api/admin/nutrizionisti").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Gia\",\"cognome\":\"Attivo\",\"email\":\"gia@studio.it\"}"))
           .andExpect(status().isConflict());
    }

    // ─────────── Invariante E — erasure account: due sole vie, nient'altro ───────────

    @Test
    @WithMockUser(username = "sa@test.it", authorities = { "SUPER_ADMIN" })
    void elimina_superAdmin_cancellaNutrizionista_204_conHookI10() throws Exception {
        Long id = seedUtente("nutr@test.it", "NUTRIS00A00A0001", "NUTRIZIONISTA", "sub-nutr").getId();
        mvc.perform(delete("/api/admin/nutrizionisti/{id}", id)).andExpect(status().isNoContent());
        assertTrue(repoUtente.findById(id).isEmpty(), "il nutrizionista deve essere cancellato");
        verify(keycloakAdminClient).deleteUser("sub-nutr"); // hook I10 cross-store (afterCommit)
    }

    @Test
    @WithMockUser(username = "sa@test.it", authorities = { "SUPER_ADMIN" })
    void elimina_targetSuperAdmin_403_guardTarget() throws Exception {
        Long id = seedUtente("sa2@test.it", "SADMIN00A00A0002", "SUPER_ADMIN", null).getId();
        mvc.perform(delete("/api/admin/nutrizionisti/{id}", id)).andExpect(status().isForbidden());
        assertTrue(repoUtente.findById(id).isPresent(), "un SUPER_ADMIN NON è eliminabile da qui");
        verify(keycloakAdminClient, never()).deleteUser(anyString());
    }

    @Test
    @WithMockUser(username = "nutr@test.it", authorities = {})
    void elimina_nonSuperAdmin_403() throws Exception {
        mvc.perform(delete("/api/admin/nutrizionisti/{id}", 1L)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "sa@test.it", authorities = { "SUPER_ADMIN" })
    void elimina_idInesistente_404() throws Exception {
        mvc.perform(delete("/api/admin/nutrizionisti/{id}", 999999L)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "self@test.it", authorities = { "UTENTE_DELETE_PROFILE" })
    void selfDelete_conPermesso_cancellaSoloSe_204() throws Exception {
        Long id = seedUtente("self@test.it", "SELFDL00A00A0003", "NUTRIZIONISTA", null).getId();
        mvc.perform(delete("/api/utenti/profilo")).andExpect(status().isNoContent());
        assertTrue(repoUtente.findById(id).isEmpty(), "il proprio account deve essere cancellato");
    }

    @Test
    @WithMockUser(username = "self@test.it", authorities = {})
    void selfDelete_senzaPermesso_403() throws Exception {
        mvc.perform(delete("/api/utenti/profilo")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "x@test.it", authorities = { "UTENTE_DELETE" })
    void deleteArbitrarioPerId_rimosso_4xx() throws Exception {
        // Item 2b: l'endpoint DELETE /api/utenti (body id) è RIMOSSO → nessun mapping (405) anche con UTENTE_DELETE.
        mvc.perform(delete("/api/utenti").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
           .andExpect(status().is4xxClientError());
    }
}
