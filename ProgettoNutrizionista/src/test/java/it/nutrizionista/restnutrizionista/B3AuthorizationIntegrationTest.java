package it.nutrizionista.restnutrizionista;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * B3 (Batch 3) — difesa in profondità: ogni endpoint deve avere un'autorizzazione ESPLICITA
 * ({@code @PreAuthorize}). Verifica a livello controller (MockMvc, filtri di sicurezza attivi): un utente
 * autenticato ma <b>senza</b> l'authority richiesta riceve 403; con l'authority (o autenticato, per il
 * catalogo di sistema) è autorizzato. Endpoint coperti: {@code PromemoriaController} ×6 (→ APPUNTAMENTO_*),
 * {@code UtenteController.uploadLogo} (→ UTENTE_PROFILE), {@code SystemController.getStandardTags} (→ isAuthenticated).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class B3AuthorizationIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL = "b3user@test.it";
	/** Body JSON valido per create/update Promemoria (testo+data obbligatori) → il @Valid passa e scatta il @PreAuthorize. */
	private static final String PROMEMORIA_JSON = "{\"testo\":\"nota\",\"data\":\"2026-01-01\",\"allDay\":true}";

	@Autowired private MockMvc mvc;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private RuoloRepository repoRuolo;

	@BeforeEach
	void seed() {
		Ruolo r = new Ruolo();
		r.setNome("NUTRIZIONISTA");
		r.setAlias("NUTRIZIONISTA");
		Ruolo ruolo = repoRuolo.save(r);

		Utente u = new Utente();
		u.setNome("B3");
		u.setCognome("Tester");
		u.setCodiceFiscale("BBBBBB00B00B000Z");
		u.setEmail(EMAIL);
		u.setPassword("x");
		u.setTelefono("000");
		u.setIndirizzo("x");
		u.setRuolo(ruolo);
		repoUtente.save(u);
	}

	// ── PromemoriaController ×6 → APPUNTAMENTO_* ──

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void promemoria_getByDateRange_senzaPermesso_403() throws Exception {
		mvc.perform(get("/api/promemoria").param("start", "2026-01-01").param("end", "2026-01-31"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL, authorities = { "APPUNTAMENTO_READ" })
	void promemoria_getByDateRange_conPermesso_200() throws Exception {
		mvc.perform(get("/api/promemoria").param("start", "2026-01-01").param("end", "2026-01-31"))
			.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void promemoria_getById_senzaPermesso_403() throws Exception {
		mvc.perform(get("/api/promemoria/1")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void promemoria_create_senzaPermesso_403() throws Exception {
		mvc.perform(post("/api/promemoria").contentType(MediaType.APPLICATION_JSON).content(PROMEMORIA_JSON))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void promemoria_update_senzaPermesso_403() throws Exception {
		mvc.perform(put("/api/promemoria/1").contentType(MediaType.APPLICATION_JSON).content(PROMEMORIA_JSON))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void promemoria_delete_senzaPermesso_403() throws Exception {
		mvc.perform(delete("/api/promemoria/1")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void promemoria_move_senzaPermesso_403() throws Exception {
		mvc.perform(patch("/api/promemoria/1/move").param("start", "2026-01-01T10:00:00"))
			.andExpect(status().isForbidden());
	}

	// ── SystemController.getStandardTags → isAuthenticated ──

	@Test
	void systemTags_senzaAutenticazione_4xx() throws Exception {
		mvc.perform(get("/api/system/tags")).andExpect(status().is4xxClientError());
	}

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void systemTags_autenticato_200() throws Exception {
		mvc.perform(get("/api/system/tags")).andExpect(status().isOk());
	}

	// ── UtenteController.uploadLogo → UTENTE_PROFILE ──

	@Test
	@WithMockUser(username = EMAIL, authorities = {})
	void uploadLogo_senzaPermesso_403() throws Exception {
		var file = new MockMultipartFile("image", "logo.png", "image/png", new byte[] { 1, 2, 3 });
		mvc.perform(multipart("/api/utenti/logo").file(file).param("utenteId", "1"))
			.andExpect(status().isForbidden());
	}
}
