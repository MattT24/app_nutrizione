package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;

import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Anti-leakage cross-tenant (QW-2 / finding A1). Verifica che un nutrizionista B, pur possedendo
 * il permesso richiesto dall'endpoint (@PreAuthorize soddisfatto), NON possa accedere alle risorse
 * (dati sanitari) del nutrizionista A: l'OwnershipValidator deve rispondere 403.
 * Il fatto che B abbia il permesso è ciò che rende il test una prova dell'ownership e non del RBAC:
 * senza il fix, B otterrebbe 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnershipAntiLeakageIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL_A = "nutriA@test.it";
	private static final String EMAIL_B = "nutriB@test.it";

	@Autowired private MockMvc mvc;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private ClienteRepository repoCliente;
	@Autowired private SchedaRepository repoScheda;
	@Autowired private CalcoloTdeeRepository repoCalcolo;
	@Autowired private DocumentoFascicoloRepository repoFascicolo;
	@Autowired private PastoRepository repoPasto;
	@Autowired private AlimentoBaseRepository repoAlimento;
	@Autowired private AlimentoPastoRepository repoAlimentoPasto;
	@Autowired private AlimentoAlternativoRepository repoAlternativa;
	@Autowired private AppuntamentoRepository repoAppuntamento;
	@Autowired private MisurazioneAntropometricaRepository repoMisurazione;
	@Autowired private PlicometriaRepository repoPlicometria;
	@Autowired private AuditLogRepository repoAudit;
	@Autowired private SchedaTemplateRepository repoSchedaTemplate;

	private Long clienteAId;
	private Long schedaAId;
	private Long calcoloAId;
	private Long documentoAId;
	// F-OWN-SWEEP: risorse aggiuntive del tenant A per lo sweep IDOR.
	private Long pastoAId;
	private Long alimentoId;
	private Long alternativaAId;
	private Long appuntamentoAId;
	private Long misurazioneAId;
	private Long plicometriaAId;
	private Long templateBId;   // SchedaTemplate di B → per raggiungere il lookup della scheda di A (deny SCHEDA + A7)
	private Long templateAId;   // SchedaTemplate di A → per il deny sul TEMPLATE (403 senza A7, by-design)

	@BeforeEach
	void seed() {
		// Nutrizionista A (proprietario delle risorse) + Nutrizionista B (l'attaccante autenticato)
		repoUtente.save(nutrizionista("A", EMAIL_A, "AAAAAA00A00A000A"));
		Utente a = repoUtente.findByEmail(EMAIL_A).orElseThrow();
		repoUtente.save(nutrizionista("B", EMAIL_B, "BBBBBB00B00B000B"));

		Cliente c = new Cliente();
		c.setSesso(Sesso.Maschio);
		c.setNome("Mario");
		c.setCognome("Rossi");
		c.setCodiceFiscale("MRARSS00A00A000A");
		c.setEmail("clienteA@test.it");
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
		c.setNutrizionista(a);
		Cliente clienteA = repoCliente.save(c);
		clienteAId = clienteA.getId();

		Scheda s = new Scheda();
		s.setCliente(clienteA);
		s.setNome("Scheda A");
		s.setAttiva(true);
		schedaAId = repoScheda.save(s).getId();

		CalcoloTdee calc = new CalcoloTdee();
		calc.setCliente(clienteA);
		calc.setDataCalcolo(LocalDate.now());
		calc.setSesso("M");
		calc.setEta(30);
		calc.setPeso(80.0);
		calc.setAltezza(180.0);
		calc.setLivelloAttivita(1.55);
		calc.setBmr(1500.0);
		calc.setTdee(2000.0);
		calcoloAId = repoCalcolo.save(calc).getId();

		DocumentoFascicolo doc = new DocumentoFascicolo();
		doc.setCliente(clienteA);
		doc.setTitolo("Documento A");
		doc.setTipoDocumento(TipoDocumento.SCHEDA);
		doc.setPercorsoFile("uploads/fascicoli/dummy-non-letto.pdf"); // mai letto: l'ownership scatta prima
		documentoAId = repoFascicolo.save(doc).getId();

		// ── F-OWN-SWEEP: albero piano (pasto→alimento→alternativa) + appuntamento/misurazione/plicometria di A ──
		AlimentoBase alimento = repoAlimento.save(aliment("Pane A"));
		AlimentoBase alimentoAlt = repoAlimento.save(aliment("Riso A"));
		alimentoId = alimento.getId();

		Pasto pasto = new Pasto();
		pasto.setScheda(s);
		pasto.setNome("Colazione");
		Pasto pastoA = repoPasto.save(pasto);
		pastoAId = pastoA.getId();

		AlimentoPasto apSaved = repoAlimentoPasto.save(new AlimentoPasto(alimento, pastoA, 100));

		AlimentoAlternativo alt = new AlimentoAlternativo();
		alt.setAlimentoPasto(apSaved);
		alt.setPasto(pastoA);
		alt.setAlimentoAlternativo(alimentoAlt);
		alt.setQuantita(120);
		alt.setPriorita(1);
		alt.setMode(AlternativeMode.CALORIE);
		alternativaAId = repoAlternativa.save(alt).getId();

		Appuntamento app = new Appuntamento();
		app.setNutrizionista(a);
		app.setCliente(clienteA);
		app.setData(LocalDate.now());
		app.setEndData(LocalDate.now());
		app.setModalita(Appuntamento.Modalita.IN_STUDIO);
		app.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
		appuntamentoAId = repoAppuntamento.save(app).getId();

		MisurazioneAntropometrica mis = new MisurazioneAntropometrica();
		mis.setCliente(clienteA);
		misurazioneAId = repoMisurazione.save(mis).getId();

		Plicometria pli = new Plicometria();
		pli.setCliente(clienteA);
		pli.setMetodo(Metodo.JACKSON_POLLOCK_3);
		plicometriaAId = repoPlicometria.save(pli).getId();

		// Batch 4 A7-DENIED (applicaAScheda): template di B (per raggiungere il lookup scheda di A) e di A (deny template)
		Utente b = repoUtente.findByEmail(EMAIL_B).orElseThrow();
		templateBId = repoSchedaTemplate.save(schedaTemplate(b)).getId();
		templateAId = repoSchedaTemplate.save(schedaTemplate(a)).getId();
	}

	private SchedaTemplate schedaTemplate(Utente owner) {
		SchedaTemplate t = new SchedaTemplate();
		t.setNome("Template di " + owner.getNome());
		t.setCreatedBy(owner);
		return t;
	}

	private static AlimentoBase aliment(String nome) {
		AlimentoBase a = new AlimentoBase();
		a.setNome(nome);
		a.setMisuraInGrammi(100.0);
		Macro m = new Macro();
		m.setAlimento(a);
		m.setCalorie(100.0); m.setProteine(10.0); m.setCarboidrati(10.0); m.setGrassi(10.0);
		a.setMacroNutrienti(m);
		return a;
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

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "SCHEDA_READ" })
	void schedaPdf_diAltroTenant_e403() throws Exception {
		mvc.perform(get("/api/schede/{id}/pdf", schedaAId))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "CLIENTE_READ" })
	void tdeeStoricoCliente_diAltroTenant_e403() throws Exception {
		mvc.perform(get("/api/tdee/cliente/{clienteId}", clienteAId))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "CLIENTE_UPDATE" })
	void tdeeDelete_diAltroTenant_e403() throws Exception {
		mvc.perform(delete("/api/tdee/{calcoloId}", calcoloAId))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "CLIENTE_READ" })
	void fascicoloDownload_diAltroTenant_e403() throws Exception {
		mvc.perform(get("/api/fascicolo/{id}/download", documentoAId))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "CLIENTE_READ" })
	void tdeeRecenti_nonEspongonoCalcoliDiAltroTenant() throws Exception {
		// B non ha calcoli propri: la lista scoped deve essere vuota (prima del fix conteneva i globali di A)
		mvc.perform(get("/api/tdee/recenti"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@WithMockUser(username = EMAIL_A, authorities = { "CLIENTE_READ" })
	void tdeeRecenti_dellOwner_vedeIProprieCalcoli() throws Exception {
		// Controllo positivo: A vede il proprio calcolo (la query scoped non lo esclude)
		mvc.perform(get("/api/tdee/recenti"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
	}

	// ═══════════ F-OWN-SWEEP ═══════════
	// (a) I 5 punti corretti dallo sweep — mutating/read cross-tenant → 403.

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "PASTO_UPDATE" })
	void aggiornaQuantitaAlimentoPasto_diAltroTenant_e403() throws Exception {
		mvc.perform(put("/api/alimenti_pasto").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pasto\":{\"id\":" + pastoAId + "},\"alimento\":{\"id\":" + alimentoId + "},\"quantita\":150}"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "PASTO_UPDATE" })
	void eliminaAssociazioneAlimentoPasto_diAltroTenant_e403() throws Exception {
		mvc.perform(delete("/api/alimenti_pasto")
				.param("pastoId", pastoAId.toString()).param("alimentoId", alimentoId.toString()))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_ALTERNATIVO_UPDATE" })
	void updateAlimentoAlternativo_diAltroTenant_e403() throws Exception {
		mvc.perform(put("/api/alimenti_alternativi").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":" + alternativaAId + ",\"alimentoPastoId\":1,\"alimentoAlternativoId\":" + alimentoId + ",\"quantita\":120}"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "PASTO_READ" })
	void listAlimentiByPasto_diAltroTenant_e403() throws Exception {
		mvc.perform(get("/api/alimenti_pasto/byPasto/{id}", pastoAId))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "CLIENTE_UPDATE" })
	void salvaDocumentoFascicolo_diAltroTenant_e403() throws Exception {
		// L'ownership del cliente ora precede il dedup → 403, niente early-return che esporrebbe il DTO.
		mvc.perform(post("/api/fascicolo/salva").contentType(MediaType.APPLICATION_JSON)
				.content("{\"clienteId\":" + clienteAId + ",\"tipoDocumento\":\"SCHEDA\",\"riferimentoId\":" + schedaAId + "}"))
			.andExpect(status().isForbidden());
	}

	// (b) Regression-guard: getOwned* già scoped ma prima NON testati (Appuntamento/Misurazione/Plicometria).

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "APPUNTAMENTO_DELETE" })
	void deleteAppuntamento_diAltroTenant_e403() throws Exception {
		mvc.perform(delete("/api/appuntamenti/{id}", appuntamentoAId)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "MISURAZIONE_ANTROPOMETRICA_READ" })
	void pdfMisurazione_diAltroTenant_e403() throws Exception {
		mvc.perform(get("/api/misurazioni_antropometriche/{id}/pdf", misurazioneAId)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "PLICOMETRIA_READ" })
	void pdfPlicometria_diAltroTenant_e403() throws Exception {
		mvc.perform(get("/api/plicometrie/{id}/pdf", plicometriaAId)).andExpect(status().isForbidden());
	}

	// (c) Il diniego su una sub-risorsa genera ora l'evento A7 ACCESS/DENIED col nuovo entityType.
	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "APPUNTAMENTO_DELETE" })
	void denyAppuntamento_registraEventoAuditDenied() throws Exception {
		mvc.perform(delete("/api/appuntamenti/{id}", appuntamentoAId)).andExpect(status().isForbidden());
		assertThat(repoAudit.findAll()).anySatisfy(a -> {
			assertThat(a.getAction()).isEqualTo(AuditAction.ACCESS);
			assertThat(a.getEsito()).isEqualTo(AuditOutcome.DENIED);
			assertThat(a.getEntityType()).isEqualTo(AuditEntityType.APPUNTAMENTO);
			assertThat(a.getEntityId()).isEqualTo(appuntamentoAId);
		});
	}

	// ═══════════ Batch 4 — A7-DENIED su AlimentoAlternativo (metodi keyed sull'id alternativa) ═══════════
	// delete/getById/set-/delete-DisplayName ora passano da getOwnedAlimentoAlternativo → 403 + A7 ACCESS/DENIED.
	// (update già coperto sopra; i due *ForAlimentoPasto sono intercettati prima dall'ownership dell'alimentoPastoId.)

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_ALTERNATIVO_DELETE" })
	void deleteAlternativa_diAltroTenant_e403() throws Exception {
		mvc.perform(delete("/api/alimenti_alternativi/{id}", alternativaAId)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_ALTERNATIVO_READ" })
	void getAlternativa_diAltroTenant_e403() throws Exception {
		mvc.perform(get("/api/alimenti_alternativi/{id}", alternativaAId)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_ALTERNATIVO_UPDATE" })
	void setDisplayNameAlternativa_diAltroTenant_e403() throws Exception {
		mvc.perform(put("/api/alimenti_alternativi/{id}/display-name", alternativaAId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_ALTERNATIVO_UPDATE" })
	void deleteDisplayNameAlternativa_diAltroTenant_e403() throws Exception {
		mvc.perform(delete("/api/alimenti_alternativi/{id}/display-name", alternativaAId)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_ALTERNATIVO_DELETE" })
	void denyAlternativa_registraEventoAuditDenied() throws Exception {
		mvc.perform(delete("/api/alimenti_alternativi/{id}", alternativaAId)).andExpect(status().isForbidden());
		assertThat(repoAudit.findAll()).anySatisfy(a -> {
			assertThat(a.getAction()).isEqualTo(AuditAction.ACCESS);
			assertThat(a.getEsito()).isEqualTo(AuditOutcome.DENIED);
			// AuditEntityType non ha ALIMENTO_ALTERNATIVO: si usa ALIMENTO_PASTO con l'id dell'alternativa (pre-esistente).
			assertThat(a.getEntityType()).isEqualTo(AuditEntityType.ALIMENTO_PASTO);
			assertThat(a.getEntityId()).isEqualTo(alternativaAId);
		});
	}

	// ═══════════ Batch 4 — applicaAScheda: 404→403 + A7 SCHEDA sulla scheda; il TEMPLATE resta 403 senza A7 ═══════════

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "SCHEDA_UPDATE" })
	void applicaTemplateSuSchedaDiAltroTenant_e403_conA7Scheda() throws Exception {
		// B possiede il template (checkOwnership passa) ma la scheda destinazione è di A → deny(SCHEDA) 403 + A7.
		mvc.perform(post("/api/schede-template/{t}/applica/{s}", templateBId, schedaAId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"REPLACE\"}"))
			.andExpect(status().isForbidden());
		assertThat(repoAudit.findAll()).anySatisfy(a -> {
			assertThat(a.getAction()).isEqualTo(AuditAction.ACCESS);
			assertThat(a.getEsito()).isEqualTo(AuditOutcome.DENIED);
			assertThat(a.getEntityType()).isEqualTo(AuditEntityType.SCHEDA);
			assertThat(a.getEntityId()).isEqualTo(schedaAId);
		});
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "SCHEDA_UPDATE" })
	void applicaTemplateAltrui_e403_senzaA7() throws Exception {
		// Il template è di A → checkOwnership fallisce PRIMA del lookup scheda: 403 sul TEMPLATE, nessuna riga A7 (by-design).
		mvc.perform(post("/api/schede-template/{t}/applica/{s}", templateAId, schedaAId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"REPLACE\"}"))
			.andExpect(status().isForbidden());
		assertThat(repoAudit.findAll()).noneSatisfy(a -> {
			assertThat(a.getAction()).isEqualTo(AuditAction.ACCESS);
			assertThat(a.getEsito()).isEqualTo(AuditOutcome.DENIED);
		});
	}
}
