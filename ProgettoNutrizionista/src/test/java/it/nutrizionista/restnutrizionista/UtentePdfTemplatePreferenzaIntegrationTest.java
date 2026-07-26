package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TemplatePdf;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/** Copre l'endpoint self-service {@code PUT /api/utenti/pdf-template} (preferenza di default, vedi Settings). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UtentePdfTemplatePreferenzaIntegrationTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri-pdf-pref@test.it";

    @Autowired private MockMvc mvc;
    @Autowired private UtenteRepository repoUtente;

    private Long utenteId;

    @BeforeEach
    void seed() {
        Utente u = new Utente();
        u.setNome("Gabriel");
        u.setCognome("Spena");
        u.setCodiceFiscale("SPNGRL01A01A000B");
        u.setEmail(EMAIL);
        u.setPassword("x");
        u.setTelefono("000");
        u.setIndirizzo("x");
        utenteId = repoUtente.save(u).getId();
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "UTENTE_PROFILE" })
    void aggiornaPreferenza_200EPersistita() throws Exception {
        mvc.perform(put("/api/utenti/pdf-template").contentType(MediaType.APPLICATION_JSON)
                .content("{\"templatePdf\":\"ESSENZIALE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templatePdfPreferito").value("ESSENZIALE"));

        assertThat(repoUtente.findById(utenteId).orElseThrow().getTemplatePdfPreferito())
                .isEqualTo(TemplatePdf.ESSENZIALE);
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "UTENTE_PROFILE" })
    void valoreMancante_400() throws Exception {
        mvc.perform(put("/api/utenti/pdf-template").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "UTENTE_PROFILE" })
    void valoreNonValido_erroreGestito() throws Exception {
        // Il parse JSON fallito dell'enum (HttpMessageNotReadableException) non ha un handler dedicato
        // in GlobalExceptionHandler → ricade nel catch-all RuntimeException (500 generico, comportamento
        // pre-esistente e uniforme per tutti i body con enum dell'API, non specifico di questa feature).
        mvc.perform(put("/api/utenti/pdf-template").contentType(MediaType.APPLICATION_JSON)
                .content("{\"templatePdf\":\"NONESISTENTE\"}"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void senzaAutenticazione_401o403() throws Exception {
        mvc.perform(put("/api/utenti/pdf-template").contentType(MediaType.APPLICATION_JSON)
                .content("{\"templatePdf\":\"ESSENZIALE\"}"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "UTENTE_PROFILE" })
    void profiloRiflettePreferenzaSalvata() throws Exception {
        mvc.perform(put("/api/utenti/pdf-template").contentType(MediaType.APPLICATION_JSON)
                .content("{\"templatePdf\":\"ESSENZIALE\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/utenti/profilo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templatePdfPreferito").value("ESSENZIALE"));
    }
}
