package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * A5.3 — Limitazione del trattamento (art. 18 GDPR). Verifica che, con un cliente "limitato":
 * <ul>
 *   <li>le operazioni che <b>scrivono/producono/inviano</b> siano rifiutate con <b>423 Locked</b>;</li>
 *   <li>le <b>letture</b> del titolare restino consentite (200);</li>
 *   <li>attiva/revoca funzionino e siano <b>auditate</b> (A7) per id del cliente;</li>
 *   <li>dopo la revoca l'operatività sia ripristinata;</li>
 *   <li>la <b>cancellazione</b> (art. 17) resti consentita anche se limitato;</li>
 *   <li><b>cross-tenant</b>: un tenant estraneo riceva <b>403</b> (ownership) e NON 423 — così non si
 *       rivela lo stato "limitato" di un cliente di un altro nutrizionista.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LimitazioneTrattamentoIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL_A = "nutriA-lim@test.it";
	private static final String EMAIL_B = "nutriB-lim@test.it";

	@Autowired private MockMvc mvc;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private ClienteRepository repoCliente;
	@Autowired private SchedaRepository repoScheda;
	@Autowired private AuditLogRepository repoAudit;
	@Autowired private AppuntamentoRepository repoAppuntamento;
	@Autowired private DocumentoFascicoloRepository repoFascicolo;

	private Long clienteLimitatoId;
	private Long clienteNormaleId;
	private Long schedaLimitatoId;
	private Long schedaNormaleId;
	private Long appuntamentoLimitatoId;
	private Long appuntamentoNormaleId;
	private Long appuntamentoManualeId;

	/** Body update appuntamento valido (allDay → salta la validazione slot; campi NOT NULL valorizzati). */
	private static final String APPUNTAMENTO_BODY =
			"{\"data\":\"2026-06-01\",\"endData\":\"2026-06-01\",\"allDay\":true,\"modalita\":\"IN_STUDIO\",\"stato\":\"PRENOTATO\"}";

	@BeforeEach
	void seed() {
		repoUtente.save(nutrizionista("A", EMAIL_A, "AAAAAA00A00A000L"));
		Utente a = repoUtente.findByEmail(EMAIL_A).orElseThrow();
		repoUtente.save(nutrizionista("B", EMAIL_B, "BBBBBB00B00B000L"));

		Cliente limitato = cliente(a, "Luca", "Limitato", "clienteLim@test.it");
		limitato.setTrattamentoLimitato(true); // seed diretto dello stato "limitato"
		clienteLimitatoId = repoCliente.save(limitato).getId();

		Cliente normale = cliente(a, "Nora", "Normale", "clienteNorm@test.it");
		clienteNormaleId = repoCliente.save(normale).getId();

		Scheda s = new Scheda();
		s.setCliente(limitato);
		s.setNome("Scheda del cliente limitato");
		s.setAttiva(true);
		schedaLimitatoId = repoScheda.save(s).getId();

		Scheda sn = new Scheda();
		sn.setCliente(normale);
		sn.setNome("Scheda del cliente normale");
		sn.setAttiva(true);
		schedaNormaleId = repoScheda.save(sn).getId();

		// Batch 4 A5.3: appuntamenti per update/delete (cliente limitato / normale / manuale-senza-cliente)
		appuntamentoLimitatoId = repoAppuntamento.save(appuntamento(a, limitato)).getId();
		appuntamentoNormaleId = repoAppuntamento.save(appuntamento(a, normale)).getId();
		appuntamentoManualeId = repoAppuntamento.save(appuntamento(a, null)).getId();
	}

	private Cliente cliente(Utente owner, String nome, String cognome, String email) {
		Cliente c = new Cliente();
		c.setSesso(Sesso.Maschio);
		c.setNome(nome);
		c.setCognome(cognome);
		c.setEmail(email);
		c.setDataNascita(LocalDate.of(1990, 1, 1));
		c.setPeso(70.0);
		c.setAltezza(175);
		c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
		c.setIntolleranze("N");
		c.setFunzioniIntestinali("N");
		c.setProblematicheSalutari("N");
		c.setQuantitaEQualitaDelSonno("N");
		c.setAssunzioneFarmaci("N");
		c.setBeveAlcol(false);
		c.setFuma(false);
		c.setNutrizionista(owner);
		return c;
	}

	private Utente nutrizionista(String suffix, String email, String cf) {
		Utente u = new Utente();
		u.setNome("Nutri" + suffix);
		u.setCognome("Test");
		u.setCodiceFiscale(cf);
		u.setEmail(email);
		u.setPassword("x");
		u.setTelefono("000");
		u.setIndirizzo("x");
		return u;
	}

	private Appuntamento appuntamento(Utente owner, Cliente cliente) {
		Appuntamento ap = new Appuntamento();
		ap.setNutrizionista(owner);
		ap.setCliente(cliente); // null = appuntamento "manuale" senza cliente registrato
		ap.setData(LocalDate.of(2026, 6, 1));
		ap.setEndData(LocalDate.of(2026, 6, 1));
		ap.setAllDay(true);
		ap.setModalita(Appuntamento.Modalita.IN_STUDIO);
		ap.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
		return ap;
	}

	private String tdeeBody(Long clienteId) {
		return "{\"clienteId\":" + clienteId + ",\"sesso\":\"M\",\"eta\":30,\"peso\":80,\"altezza\":180,\"livelloAttivita\":1.55}";
	}

	// ═══════════ BLOCCO 423 su scrittura/produzione/invio ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "SCHEDA_CREATE" })
	void createScheda_perClienteLimitato_e423() throws Exception {
		mvc.perform(post("/api/schede").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Nuova\",\"cliente\":{\"id\":" + clienteLimitatoId + "}}"))
			.andExpect(status().isLocked());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "MISURAZIONE_ANTROPOMETRICA_CREATE" })
	void createMisurazione_perClienteLimitato_e423() throws Exception {
		mvc.perform(post("/api/misurazioni_antropometriche").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cliente\":{\"id\":" + clienteLimitatoId + "}}"))
			.andExpect(status().isLocked());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "CLIENTE_UPDATE" })
	void tdee_perClienteLimitato_e423() throws Exception {
		mvc.perform(post("/api/tdee").contentType(MediaType.APPLICATION_JSON).content(tdeeBody(clienteLimitatoId)))
			.andExpect(status().isLocked());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "CLIENTE_UPDATE" })
	void salvaFascicolo_perClienteLimitato_e423() throws Exception {
		// Produzione+persistenza di un nuovo PDF nel fascicolo → bloccata.
		mvc.perform(post("/api/fascicolo/salva").contentType(MediaType.APPLICATION_JSON)
				.content("{\"clienteId\":" + clienteLimitatoId + ",\"tipoDocumento\":\"SCHEDA\",\"riferimentoId\":" + schedaLimitatoId + "}"))
			.andExpect(status().isLocked());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "SCHEDA_READ" })
	void shareScheda_perClienteLimitato_e423() throws Exception {
		// Outbound (invio a terzi) → bloccato.
		mvc.perform(post("/api/schede/{id}/share", schedaLimitatoId).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isLocked());
	}

	// ═══════════ LETTURA del titolare consentita ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "CLIENTE_DETTAGLIO" })
	void dettaglioCliente_limitato_restaLeggibile() throws Exception {
		mvc.perform(post("/api/clienti/dettaglio").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":" + clienteLimitatoId + "}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trattamentoLimitato").value(true));
	}

	// ═══════════ Controllo positivo: cliente NON limitato opera ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "CLIENTE_UPDATE" })
	void tdee_perClienteNonLimitato_ok() throws Exception {
		mvc.perform(post("/api/tdee").contentType(MediaType.APPLICATION_JSON).content(tdeeBody(clienteNormaleId)))
			.andExpect(status().is2xxSuccessful());
	}

	// ═══════════ attiva → audit → blocco → revoca → audit → ripristino ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "CLIENTE_UPDATE" })
	void attivaERevocaLimitazione_audit_eRipristinoOperativita() throws Exception {
		// Attiva: il cliente prima operativo diventa limitato.
		mvc.perform(patch("/api/clienti/{id}/limitazione", clienteNormaleId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"Richiesta dell'interessato\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trattamentoLimitato").value(true));

		assertThat(repoAudit.findAll()).anySatisfy(a -> {
			assertThat(a.getAction()).isEqualTo(AuditAction.LIMITAZIONE_ATTIVATA);
			assertThat(a.getClienteId()).isEqualTo(clienteNormaleId);
		});

		// Ora una scrittura è bloccata (423).
		mvc.perform(post("/api/tdee").contentType(MediaType.APPLICATION_JSON).content(tdeeBody(clienteNormaleId)))
			.andExpect(status().isLocked());

		// Revoca: torna operativo.
		mvc.perform(patch("/api/clienti/{id}/limitazione/revoca", clienteNormaleId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.trattamentoLimitato").value(false));

		assertThat(repoAudit.findAll()).anySatisfy(a -> {
			assertThat(a.getAction()).isEqualTo(AuditAction.LIMITAZIONE_REVOCATA);
			assertThat(a.getClienteId()).isEqualTo(clienteNormaleId);
		});

		// Operatività ripristinata.
		mvc.perform(post("/api/tdee").contentType(MediaType.APPLICATION_JSON).content(tdeeBody(clienteNormaleId)))
			.andExpect(status().is2xxSuccessful());
	}

	// ═══════════ art. 17 prevale: delete consentito anche se limitato ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "CLIENTE_MY_DELETE" })
	void deleteMyCliente_ancheSeLimitato_funziona() throws Exception {
		mvc.perform(delete("/api/clienti/mio").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":" + clienteLimitatoId + "}"))
			.andExpect(status().isNoContent());
		assertThat(repoCliente.findById(clienteLimitatoId)).isEmpty();
	}

	// ═══════════ Cross-tenant: 403 (ownership), NON 423 (nessun leak dello stato limitato) ═══════════

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "CLIENTE_UPDATE" })
	void crossTenant_attivaLimitazione_e403() throws Exception {
		mvc.perform(patch("/api/clienti/{id}/limitazione", clienteLimitatoId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "CLIENTE_UPDATE" })
	void crossTenant_scritturaSuClienteLimitato_e403_non423() throws Exception {
		// Il tenant estraneo deve ricevere 403 (l'ownership precede il check di limitazione):
		// un 423 rivelerebbe che il cliente di un ALTRO nutrizionista è "limitato".
		mvc.perform(post("/api/tdee").contentType(MediaType.APPLICATION_JSON).content(tdeeBody(clienteLimitatoId)))
			.andExpect(status().isForbidden());
	}

	// ═══════════ MealService (/api/meals) — falla di enforcement chiusa (addendum) ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "MEAL_CREATE" })
	void creaPasto_perClienteLimitato_e423() throws Exception {
		mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON)
				.content("{\"schedaId\":" + schedaLimitatoId + ",\"nome\":\"Spuntino\"}"))
			.andExpect(status().isLocked());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "MEAL_CREATE" })
	void creaPasto_crossTenant_e403_non423() throws Exception {
		mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON)
				.content("{\"schedaId\":" + schedaLimitatoId + ",\"nome\":\"Spuntino\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "MEAL_CREATE" })
	void creaPasto_perClienteNonLimitato_ok() throws Exception {
		mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON)
				.content("{\"schedaId\":" + schedaNormaleId + ",\"nome\":\"Spuntino\"}"))
			.andExpect(status().is2xxSuccessful());
	}

	// ═══════════ A5.3 × auto-sync fascicolo: la scrittura bloccata NON auto-produce un PDF ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "MEAL_CREATE" })
	void scritturaBloccata_perClienteLimitato_nonAutoProduceDocumentoFascicolo() throws Exception {
		// Su cliente NON limitato questa scrittura innescherebbe l'auto-save del PDF nel fascicolo
		// (afterCommit). Sul limitato la scrittura è 423 PRIMA del commit → l'auto-save non gira mai
		// e nessun documento sanitario viene prodotto (art. 18: niente nuova produzione).
		mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON)
				.content("{\"schedaId\":" + schedaLimitatoId + ",\"nome\":\"Spuntino\"}"))
			.andExpect(status().isLocked());
		assertThat(repoFascicolo.findByClienteIdOrderByDataCreazioneDesc(clienteLimitatoId))
			.as("nessun documento auto-generato nel fascicolo di un cliente limitato")
			.isEmpty();
	}

	// ═══════════ Batch 4 — A5.3 su AppuntamentoService: update + delete bloccati (decisione BLOCCA 2026-07-20) ═══════════

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "APPUNTAMENTO_UPDATE" })
	void updateAppuntamento_diClienteLimitato_e423() throws Exception {
		// Il check è sul cliente ESISTENTE dell'appuntamento (il body non aggancia clienteId).
		mvc.perform(put("/api/appuntamenti/{id}", appuntamentoLimitatoId).contentType(MediaType.APPLICATION_JSON)
				.content(APPUNTAMENTO_BODY))
			.andExpect(status().isLocked());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "APPUNTAMENTO_DELETE" })
	void deleteAppuntamento_diClienteLimitato_e423() throws Exception {
		// Annullamento (soft-delete) di un appuntamento di cliente limitato → 423.
		mvc.perform(delete("/api/appuntamenti/{id}", appuntamentoLimitatoId))
			.andExpect(status().isLocked());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "APPUNTAMENTO_UPDATE" })
	void crossTenant_updateAppuntamento_e403_non423() throws Exception {
		// Ownership precede la limitazione: il tenant estraneo riceve 403, non 423 (nessun leak dello stato).
		mvc.perform(put("/api/appuntamenti/{id}", appuntamentoLimitatoId).contentType(MediaType.APPLICATION_JSON)
				.content(APPUNTAMENTO_BODY))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "APPUNTAMENTO_DELETE" })
	void crossTenant_deleteAppuntamento_e403_non423() throws Exception {
		mvc.perform(delete("/api/appuntamenti/{id}", appuntamentoLimitatoId))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "APPUNTAMENTO_UPDATE" })
	void updateAppuntamento_diClienteNonLimitato_ok() throws Exception {
		mvc.perform(put("/api/appuntamenti/{id}", appuntamentoNormaleId).contentType(MediaType.APPLICATION_JSON)
				.content(APPUNTAMENTO_BODY))
			.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "APPUNTAMENTO_DELETE" })
	void deleteAppuntamento_manualeSenzaCliente_ok() throws Exception {
		// Appuntamento senza cliente registrato: assertNonLimitato(null) è no-op → nessun 423 spurio.
		mvc.perform(delete("/api/appuntamenti/{id}", appuntamentoManualeId))
			.andExpect(status().isNoContent());
	}
}
