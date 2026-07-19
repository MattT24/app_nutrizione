package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Permesso;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.RuoloPermesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.CredenzialeDemoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PermessoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloPermessoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.AdminDemoAccountService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/** Test end-to-end del confine di sicurezza demo su H2 isolato. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoAccountSecurityIntegrationTest extends SafeTestDatabaseBase {
    private static final String ADMIN_PASSWORD = "Admin-Test-2026!";
    private static final String MASTER_PASSWORD = "Master-Test-2026!#";
    private static final String DEMO_PASSWORD = "Demo-Test-2026!#";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UtenteRepository utenteRepository;
    @Autowired private RuoloRepository ruoloRepository;
    @Autowired private PermessoRepository permessoRepository;
    @Autowired private RuoloPermessoRepository ruoloPermessoRepository;
    @Autowired private CredenzialeDemoRepository demoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private AdminDemoAccountService adminDemoAccountService;
    @Autowired private MutableClock clock;

    @TestConfiguration
    static class ClockTestConfig {
        @Bean @Primary
        MutableClock mutableClock() {
            // Nel futuro rispetto all'orologio reale: JJWT non considera già scaduti i token del test.
            return new MutableClock(Instant.parse("2030-01-01T10:00:00Z"));
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void avanzaGiorni(long giorni) { instant = instant.plusSeconds(giorni * 86_400); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    @BeforeEach
    void setup() {
        clock.instant = Instant.parse("2030-01-01T10:00:00Z");
        Ruolo superAdmin = ruolo("SUPER_ADMIN", "Super Admin");
        Ruolo nutrizionista = ruolo("NUTRIZIONISTA", "Nutrizionista");
        collega(superAdmin, permesso("SUPER_ADMIN", "Super Admin"));
        collega(nutrizionista, permesso("UTENTE_PROFILE", "Profilo utente"));
        collega(nutrizionista, permesso("ASSISTENZA_USE", "Richiedi assistenza"));

        Utente admin = new Utente();
        admin.setNome("Super");
        admin.setCognome("Admin");
        admin.setCodiceFiscale("TESTSUPERADMIN01");
        admin.setEmail("superadmin@test.local");
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setTelefono("-");
        admin.setIndirizzo("-");
        admin.setRuolo(superAdmin);
        utenteRepository.save(admin);

        ReflectionTestUtils.setField(adminDemoAccountService, "masterPasswordHash",
                passwordEncoder.encode(MASTER_PASSWORD));
    }

    @Test
    void cicloCompleto_scadenzaRevocaEImpersonazioneSonoServerSide() throws Exception {
        String adminToken = loginAdmin();
        String createBody = """
                {"username":"demo-produzione","password":"%s"}
                """.formatted(DEMO_PASSWORD);
        String createJson = mvc.perform(post("/api/admin/demo-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("demo-produzione"))
                .andExpect(jsonPath("$.stato").value("ATTIVO"))
                .andReturn().getResponse().getContentAsString();
        long demoId = objectMapper.readTree(createJson).get("id").asLong();

        String loginJson = mvc.perform(post("/api/auth/demo/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo-produzione\",\"password\":\"" + DEMO_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String demoToken = objectMapper.readTree(loginJson).get("token").asText();

        mvc.perform(get("/api/assistenza/tickets/attivo")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isNoContent());

        String emailTecnica = demoRepository.findConUtenteById(demoId).orElseThrow()
                .getUtente().getEmail();
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(emailTecnica))
                .isInstanceOf(UsernameNotFoundException.class);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/utenti/password")
                        .header("Authorization", "Bearer " + demoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Nuova-Test-2026!#\",\"confermaPassword\":\"Nuova-Test-2026!#\"}"))
                .andExpect(status().isBadRequest());

        clock.avanzaGiorni(15);
        mvc.perform(get("/api/assistenza/tickets/attivo")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEMO_SCADUTO"));
        mvc.perform(post("/api/auth/demo/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo-produzione\",\"password\":\"" + DEMO_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(demoRepository.findById(demoId)).isPresent();

        String impersonationBody = """
                {"adminPassword":"%s","masterPassword":"%s","motivo":"Assistenza tecnica autorizzata"}
                """.formatted(ADMIN_PASSWORD, MASTER_PASSWORD);
        String impersonationJson = mvc.perform(post("/api/admin/demo-accounts/{id}/impersonate", demoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(impersonationBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String impersonationToken = objectMapper.readTree(impersonationJson).get("token").asText();

        mvc.perform(get("/api/assistenza/tickets/attivo")
                        .header("Authorization", "Bearer " + impersonationToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/assistenza/tickets/conteggi")
                        .header("Authorization", "Bearer " + impersonationToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void impersonazioneRichiedeEntrambiISegretiEPasswordForte() throws Exception {
        String adminToken = loginAdmin();
        mvc.perform(post("/api/admin/demo-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo-debole\",\"password\":\"passwordtroppodebole\"}"))
                .andExpect(status().isBadRequest());

        String created = mvc.perform(post("/api/admin/demo-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo-sicuro\",\"password\":\"" + DEMO_PASSWORD + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();
        mvc.perform(post("/api/admin/demo-accounts/{id}/impersonate", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminPassword\":\"" + ADMIN_PASSWORD
                                + "\",\"masterPassword\":\"errata\",\"motivo\":\"Tentativo non autorizzato\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/admin/demo-accounts/{id}/disable", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/demo-accounts/{id}/impersonate", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminPassword\":\"" + ADMIN_PASSWORD
                                + "\",\"masterPassword\":\"" + MASTER_PASSWORD
                                + "\",\"motivo\":\"Assistenza account disabilitato\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void superAdminVedeConteggioEClientiRegistratiSenzaDatiSanitari() throws Exception {
        String adminToken = loginAdmin();
        String created = mvc.perform(post("/api/admin/demo-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo-clienti\",\"password\":\"" + DEMO_PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroClientiRegistrati").value(0))
                .andReturn().getResponse().getContentAsString();
        long demoId = objectMapper.readTree(created).get("id").asLong();
        Utente demoUser = demoRepository.findConUtenteById(demoId).orElseThrow().getUtente();
        mvc.perform(get("/api/admin/nutrizionisti/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaleNutrizionisti").value(0));


        Cliente cliente = new Cliente();
        cliente.setNome("Mario");
        cliente.setCognome("Rossi");
        cliente.setEmail("mario.demo@test.local");
        cliente.setTelefono("3330000000");
        cliente.setIntolleranze("lattosio");
        cliente.setFunzioniIntestinali("regolari");
        cliente.setProblematicheSalutari("dato riservato");
        cliente.setQuantitaEQualitaDelSonno("buona");
        cliente.setAssunzioneFarmaci("nessuno");
        cliente.setBeveAlcol(false);
        cliente.setFuma(false);
        cliente.setNutrizionista(demoUser);
        clienteRepository.saveAndFlush(cliente);

        mvc.perform(get("/api/admin/demo-accounts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].numeroClientiRegistrati").value(1));
        mvc.perform(get("/api/admin/demo-accounts/{id}/clienti", demoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaleElementi").value(1))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Mario"))
                .andExpect(jsonPath("$.contenuto[0].problematicheSalutari").doesNotExist())
                .andExpect(jsonPath("$.contenuto[0].peso").doesNotExist());
        mvc.perform(get("/api/admin/demo-accounts/{id}/clienti", demoId)
                        .param("q", "mario.demo")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaleElementi").value(1))
                .andExpect(jsonPath("$.numeroPagina").value(0));
        mvc.perform(get("/api/admin/demo-accounts/{id}/clienti", demoId)
                        .param("q", "x".repeat(101))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }


    @Test
    void loginStandardEmailRestaCompatibile() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"superadmin@test.local\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private String loginAdmin() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"superadmin@test.local\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private Ruolo ruolo(String alias, String nome) {
        Ruolo ruolo = new Ruolo();
        ruolo.setAlias(alias);
        ruolo.setNome(nome);
        return ruoloRepository.save(ruolo);
    }
    private Permesso permesso(String alias, String nome) {
        Permesso p = new Permesso();
        p.setAlias(alias);
        p.setNome(nome);
        return permessoRepository.save(p);
    }
    private void collega(Ruolo ruolo, Permesso permesso) {
        ruoloPermessoRepository.save(new RuoloPermesso(ruolo, permesso));
    }
}
