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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MealDefaultsIntegrationTest extends SafeTestDatabaseBase {
	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper om;
	@Autowired
	private UtenteRepository repoUtente;
	@Autowired
	private ClienteRepository repoCliente;

	private Utente nutrizionista;
	private Cliente cliente;

	@BeforeEach
	void setup() {
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
		nutrizionista = repoUtente.save(u);

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
		cliente = repoCliente.save(c);
	}

	@Test
	@WithMockUser(username = "nutri@test.it", authorities = { "SCHEDA_CREATE", "PASTO_UPDATE" })
	void createScheda_addsDefaultMeals_andDefaultMealsCannotBeDeletedViaMealsApi() throws Exception {
		String body = """
				{
				  "nome": "Scheda Test",
				  "cliente": { "id": %d },
				  "attiva": true
				}
				""".formatted(cliente.getId());

		String response = mvc.perform(
				post("/api/schede")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode root = om.readTree(response);
		JsonNode pasti = root.get("pasti");
		if (pasti == null || !pasti.isArray() || pasti.size() != 4) {
			throw new AssertionError("Attesi 4 pasti default in risposta");
		}
		
		assertDefaultMealTime(pasti, "Colazione", "07:00:00", "09:00:00");
		assertDefaultMealTime(pasti, "Pranzo", "12:30:00", "14:30:00");
		assertDefaultMealTime(pasti, "Merenda", "16:30:00", "17:30:00");
		assertDefaultMealTime(pasti, "Cena", "19:30:00", "21:30:00");

		Long defaultMealId = pasti.get(0).get("id").asLong();
		
		String updateOrariBody = """
				{ "orarioInizio": "08:00:00", "orarioFine": "09:15:00" }
				""";
		mvc.perform(put("/api/pasti/{id}/orari", defaultMealId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateOrariBody))
			.andExpect(status().isOk());
		
		mvc.perform(delete("/api/meals/{id}", defaultMealId))
				.andExpect(status().isForbidden());
	}
	
	private static void assertDefaultMealTime(JsonNode pasti, String nome, String expectedStart, String expectedEnd) {
		for (JsonNode p : pasti) {
			if (nome.equalsIgnoreCase(p.get("nome").asText())) {
				String start = p.get("orarioInizio").asText();
				String end = p.get("orarioFine").asText();
				if (!expectedStart.equals(start) || !expectedEnd.equals(end)) {
					throw new AssertionError("Orari default non validi per " + nome + ": " + start + "-" + end);
				}
				return;
			}
		}
		throw new AssertionError("Pasto non trovato in risposta: " + nome);
	}
}
