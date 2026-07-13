package it.nutrizionista.restnutrizionista;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RouteProtectionIntegrationTest extends SafeTestDatabaseBase {
	@Autowired private MockMvc mvc;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private ClienteRepository repoCliente;
	@Autowired private SchedaRepository repoScheda;
	@Autowired private PastoRepository repoPasto;
	@Autowired private AlimentoBaseRepository repoAlimento;
	@Autowired private AlimentoPastoRepository repoAlimentoPasto;

	private Long schedaId;
	private Long customMealId;
	private Long alimentoPastoId;

	@BeforeEach
	void setup() {
		repoAlimentoPasto.deleteAll();
		repoAlimento.deleteAll();
		repoPasto.deleteAll();
		repoScheda.deleteAll();
		repoCliente.deleteAll();
		repoUtente.deleteAll();

		Utente u = new Utente();
		u.setNome("Test");
		u.setCognome("Nutrizionista");
		u.setCodiceFiscale("TTSTNT00A00A000A");
		u.setEmail("nutri@test.it");
		u.setPassword("x");
		u.setTelefono("000");
		u.setIndirizzo("x");
		Utente nutrizionista = repoUtente.save(u);

		Cliente c = new Cliente();
		c.setSesso(Sesso.Maschio);
		c.setNome("Mario");
		c.setCognome("Rossi");
		c.setCodiceFiscale("MRARSS00A00A000A");
		c.setEmail("cliente@test.it");
		c.setDataNascita(LocalDate.of(1990, 1, 1));
		c.setPeso(70.0);
		c.setAltezza(175);
		c.setLivelloDiAttivita(it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita.SEDENTARIO);
		c.setIntolleranze("N");
		c.setFunzioniIntestinali("N");
		c.setProblematicheSalutari("N");
		c.setQuantitaEQualitaDelSonno("N");
		c.setAssunzioneFarmaci("N");
		c.setBeveAlcol(false);
		c.setFuma(false);
		c.setNutrizionista(nutrizionista);
		Cliente cliente = repoCliente.save(c);

		Scheda s = new Scheda();
		s.setCliente(cliente);
		s.setNome("Scheda Test");
		s.setAttiva(true);
		Scheda savedScheda = repoScheda.save(s);
		schedaId = savedScheda.getId();

		Pasto meal = new Pasto();
		meal.setScheda(savedScheda);
		meal.setNome("Spuntino");
		meal.setDefaultCode(null);
		meal.setEliminabile(true);
		meal.setOrdineVisualizzazione(10);
		Pasto savedMeal = repoPasto.save(meal);
		customMealId = savedMeal.getId();

		AlimentoBase a = new AlimentoBase();
		a.setNome("TestFood");
		a.setMisuraInGrammi(100.0);
		Macro m = new Macro();
		m.setAlimento(a);
		m.setCalorie(100.0);
		m.setProteine(10.0);
		m.setCarboidrati(10.0);
		m.setGrassi(10.0);
		a.setMacroNutrienti(m);
		AlimentoBase savedAlimento = repoAlimento.save(a);

		AlimentoPasto ap = new AlimentoPasto(savedAlimento, savedMeal, 100);
		AlimentoPasto savedAp = repoAlimentoPasto.save(ap);
		alimentoPastoId = savedAp.getId();
	}

	@Test
	@WithMockUser(username = "nutri@test.it")
	void mealCreate_isForbiddenWithoutPermission() throws Exception {
		String body = """
				{
				  "schedaId": %d,
				  "nome": "Pasto Custom",
				  "descrizione": "x",
				  "ordineVisualizzazione": 20
				}
				""".formatted(schedaId);

		mvc.perform(post("/api/meals")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "nutri@test.it", authorities = { "MEAL_CREATE" })
	void mealCreate_isOkWithPermission() throws Exception {
		String body = """
				{
				  "schedaId": %d,
				  "nome": "Pasto Custom",
				  "descrizione": "x",
				  "ordineVisualizzazione": 20
				}
				""".formatted(schedaId);

		mvc.perform(post("/api/meals")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "nutri@test.it")
	void mealUpdate_isForbiddenWithoutPermission() throws Exception {
		String body = """
				{ "nome": "Nuovo Nome", "descrizione": "x", "ordineVisualizzazione": 20 }
				""";

		mvc.perform(put("/api/meals/{id}", customMealId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "nutri@test.it", authorities = { "MEAL_UPDATE" })
	void mealUpdate_isOkWithPermission() throws Exception {
		String body = """
				{ "nome": "Nuovo Nome", "descrizione": "x", "ordineVisualizzazione": 20 }
				""";

		mvc.perform(put("/api/meals/{id}", customMealId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "nutri@test.it")
	void mealDelete_isForbiddenWithoutPermission() throws Exception {
		mvc.perform(delete("/api/meals/{id}", customMealId))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "nutri@test.it", authorities = { "MEAL_DELETE" })
	void mealDelete_isNoContentWithPermission() throws Exception {
		mvc.perform(delete("/api/meals/{id}", customMealId))
			.andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser(username = "nutri@test.it")
	void displayName_isForbiddenWithoutSchedaUpdate() throws Exception {
		String body = "{ \"nome\": \"Nome Custom\" }";
		mvc.perform(put("/api/schede/{schedaId}/alimenti-pasto/{alimentoPastoId}/display-name", schedaId, alimentoPastoId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "nutri@test.it", authorities = { "SCHEDA_UPDATE" })
	void displayName_isOkWithSchedaUpdate() throws Exception {
		String body = "{ \"nome\": \"Nome Custom\" }";
		mvc.perform(put("/api/schede/{schedaId}/alimenti-pasto/{alimentoPastoId}/display-name", schedaId, alimentoPastoId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());
	}
}
