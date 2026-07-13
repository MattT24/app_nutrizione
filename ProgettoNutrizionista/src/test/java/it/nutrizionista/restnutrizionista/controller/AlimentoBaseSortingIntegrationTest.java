package it.nutrizionista.restnutrizionista.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlimentoBaseSortingIntegrationTest extends SafeTestDatabaseBase {

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper objectMapper;
  @Autowired
  AlimentoBaseRepository alimentoBaseRepository;
  @Autowired
  UtenteRepository utenteRepository;

  @BeforeEach
  void setup() {
    alimentoBaseRepository.deleteAll();
    utenteRepository.deleteAll();

    // getCurrentUtente() risolve il principal del @WithMockUser sull'email dell'Utente in DB
    // → serve un Utente persistito col nome utente atteso (stesso pattern degli altri integration test).
    Utente u = new Utente();
    u.setNome("Test");
    u.setCognome("Nutrizionista");
    u.setCodiceFiscale("TTSTNT00A00A000A");
    u.setEmail("nutri@test.it");
    u.setPassword("x");
    u.setTelefono("000");
    u.setIndirizzo("x");
    utenteRepository.save(u);

    alimentoBaseRepository.saveAll(List.of(
        alimento("banana"),
        alimento("Apple"),
        alimento("carota")));
  }

  @Test
  @WithMockUser(username = "nutri@test.it", authorities = "ALIMENTO_READ")
  void listAll_defaultsToNomeAscIgnoreCase_whenNoSortProvided() throws Exception {
    MvcResult result = mvc.perform(get("/api/alimenti_base")
        .param("page", "0")
        .param("size", "50"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    List<String> nomi = List.of(
        root.at("/contenuto/0/nome").asText(),
        root.at("/contenuto/1/nome").asText(),
        root.at("/contenuto/2/nome").asText());

    assertThat(nomi).containsExactly("Apple", "banana", "carota");
  }

  @Test
  @WithMockUser(username = "nutri@test.it", authorities = "ALIMENTO_READ")
  void listAll_respectsExplicitSortDesc() throws Exception {
    MvcResult result = mvc.perform(get("/api/alimenti_base")
        .param("page", "0")
        .param("size", "50")
        .param("sort", "nome,desc"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    List<String> nomi = List.of(
        root.at("/contenuto/0/nome").asText(),
        root.at("/contenuto/1/nome").asText(),
        root.at("/contenuto/2/nome").asText());

    assertThat(nomi).containsExactly("carota", "banana", "Apple");
  }

  private static AlimentoBase alimento(String nome) {
    AlimentoBase a = new AlimentoBase();
    a.setNome(nome);
    a.setMisuraInGrammi(100.0);

    Macro m = new Macro();
    m.setAlimento(a);
    m.setCalorie(100.0);
    m.setProteine(10.0);
    m.setCarboidrati(10.0);
    m.setGrassi(10.0);
    a.setMacroNutrienti(m);

    return a;
  }
}
