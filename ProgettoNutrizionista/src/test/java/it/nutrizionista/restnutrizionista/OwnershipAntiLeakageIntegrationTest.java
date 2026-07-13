package it.nutrizionista.restnutrizionista;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.OrariStudioRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
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
	@Autowired private OrariStudioRepository repoOrari;
	@Autowired private DocumentoFascicoloRepository repoFascicolo;

	private Long clienteAId;
	private Long schedaAId;
	private Long calcoloAId;
	private Long orariAId;
	private Long documentoAId;

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

		OrariStudio o = new OrariStudio();
		o.setNutrizionista(a);
		o.setGiornoSettimana(DayOfWeek.MONDAY);
		o.setGiornoLavorativo(true);
		orariAId = repoOrari.save(o).getId();

		DocumentoFascicolo doc = new DocumentoFascicolo();
		doc.setCliente(clienteA);
		doc.setTitolo("Documento A");
		doc.setTipoDocumento(TipoDocumento.SCHEDA);
		doc.setPercorsoFile("uploads/fascicoli/dummy-non-letto.pdf"); // mai letto: l'ownership scatta prima
		documentoAId = repoFascicolo.save(doc).getId();
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
	@WithMockUser(username = EMAIL_B, authorities = { "ORARI_STUDIO_DELETE" })
	void orariStudioDelete_diAltroTenant_e403() throws Exception {
		mvc.perform(delete("/api/orari_studio/{id}", orariAId))
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
}
