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
import it.nutrizionista.restnutrizionista.enums.TemaPdf;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/** Copre l'endpoint self-service {@code PUT /api/utenti/pdf-tema-colore} (preferenza di default, vedi Settings). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UtentePdfTemaColorePreferenzaIntegrationTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri-pdf-tema@test.it";

    @Autowired private MockMvc mvc;
    @Autowired private UtenteRepository repoUtente;

    private Long utenteId;

    @BeforeEach
    void seed() {
        Utente u = new Utente();
        u.setNome("Gabriel");
        u.setCognome("Spena");
        u.setCodiceFiscale("SPNGRL02A02A000C");
        u.setEmail(EMAIL);
        u.setPassword("x");
        u.setTelefono("000");
        u.setIndirizzo("x");
        utenteId = repoUtente.save(u).getId();
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "UTENTE_PROFILE" })
    void aggiornaPreferenza_200EPersistita() throws Exception {
        mvc.perform(put("/api/utenti/pdf-tema-colore").contentType(MediaType.APPLICATION_JSON)
                .content("{\"temaColore\":\"ROSSO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pdfTemaColore").value("ROSSO"));

        assertThat(repoUtente.findById(utenteId).orElseThrow().getPdfTemaColore())
                .isEqualTo(TemaPdf.ROSSO);
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "UTENTE_PROFILE" })
    void valoreMancante_400() throws Exception {
        mvc.perform(put("/api/utenti/pdf-tema-colore").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void senzaAutenticazione_401o403() throws Exception {
        mvc.perform(put("/api/utenti/pdf-tema-colore").contentType(MediaType.APPLICATION_JSON)
                .content("{\"temaColore\":\"ROSSO\"}"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "UTENTE_PROFILE" })
    void profiloRiflettePreferenzaSalvata() throws Exception {
        mvc.perform(put("/api/utenti/pdf-tema-colore").contentType(MediaType.APPLICATION_JSON)
                .content("{\"temaColore\":\"ROSSO\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/utenti/profilo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pdfTemaColore").value("ROSSO"));
    }
}
