package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Generazione PDF end-to-end attraverso il TemplateEngine REALE autoconfigurato da Spring Boot
 * (SpEL), non quello ricostruito a mano in PdfPreviewGeneratorTest. Nasce da un bug di produzione:
 * l'endpoint {@code /misurazioni_antropometriche/{id}/pdf} andava in 500 perché la chiave "hero"
 * era presente solo sulla prima card KPI — sotto OGNL (motore precedente) l'accesso a una chiave
 * assente di una Map restituiva null, sotto SpEL (motore attuale) lancia un'eccezione. Il generatore
 * di anteprime usava un motore costruito a mano che, pur avendo lo stesso TemplateMode, non
 * replicava questa differenza finché non è stato aggiornato a usare SpringTemplateEngine — questi
 * test colpiscono invece il bean reale, autorizzato, tramite MockMvc: la stessa identica richiesta
 * che ha rotto in produzione.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfGenerationIntegrationTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri-pdf@test.it";

    @Autowired private MockMvc mvc;
    @Autowired private UtenteRepository repoUtente;
    @Autowired private ClienteRepository repoCliente;
    @Autowired private MisurazioneAntropometricaRepository repoMisurazione;
    @Autowired private PlicometriaRepository repoPlicometria;
    @Autowired private SchedaRepository repoScheda;
    @Autowired private PastoRepository repoPasto;
    @Autowired private AlimentoBaseRepository repoAlimento;
    @Autowired private AlimentoPastoRepository repoAlimentoPasto;

    private Long misurazionePrimaId;   // senza precedente: card KPI "pending", niente delta
    private Long misurazioneConDeltaId; // con precedente: card KPI "hero" con delta, trend valorizzato
    private Long plicometriaId;
    private Long schedaId;

    @BeforeEach
    void seed() {
        Utente nutrizionista = new Utente();
        nutrizionista.setNome("Gabriel");
        nutrizionista.setCognome("Spena");
        nutrizionista.setCodiceFiscale("SPNGRL00A00A000A");
        nutrizionista.setEmail(EMAIL);
        nutrizionista.setPassword("x");
        nutrizionista.setTelefono("000");
        nutrizionista.setIndirizzo("x");
        Utente nut = repoUtente.save(nutrizionista);

        Cliente c = new Cliente();
        c.setSesso(Sesso.Maschio);
        c.setNome("Vincenzo");
        c.setCognome("Gallo");
        c.setCodiceFiscale("GLLVCN00A00A000A");
        c.setEmail("cliente-pdf@test.it");
        c.setDataNascita(LocalDate.of(1990, 5, 12));
        c.setPeso(92.0);
        c.setAltezza(180);
        c.setLivelloDiAttivita(it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita.SEDENTARIO);
        c.setIntolleranze("N");
        c.setFunzioniIntestinali("N");
        c.setProblematicheSalutari("N");
        c.setQuantitaEQualitaDelSonno("N");
        c.setAssunzioneFarmaci("N");
        c.setBeveAlcol(false);
        c.setFuma(false);
        c.setNutrizionista(nut);
        Cliente cliente = repoCliente.save(c);

        // Prima rilevazione: nessun valore misurato -> tutte le card KPI in stato "pending".
        MisurazioneAntropometrica prima = new MisurazioneAntropometrica();
        prima.setCliente(cliente);
        prima.setDataMisurazione(LocalDate.of(2026, 5, 15));
        prima.setPeso(90.0);
        prima.setVita(90.0);
        prima.setFianchi(100.0);
        misurazionePrimaId = repoMisurazione.save(prima).getId();

        // Seconda rilevazione più recente: attiva il confronto -> card "hero" con delta e trend.
        MisurazioneAntropometrica confronto = new MisurazioneAntropometrica();
        confronto.setCliente(cliente);
        confronto.setDataMisurazione(LocalDate.of(2026, 6, 15));
        confronto.setPeso(88.0);
        confronto.setVita(88.0);
        confronto.setFianchi(98.0);
        misurazioneConDeltaId = repoMisurazione.save(confronto).getId();

        Plicometria pli = new Plicometria();
        pli.setCliente(cliente);
        pli.setMetodo(Metodo.JACKSON_POLLOCK_3);
        pli.setDataMisurazione(LocalDate.of(2026, 6, 15));
        pli.setTricipite(18.0);
        pli.setSovrailiaca(16.0);
        pli.setCoscia(24.0);
        pli.setPercentualeMassaGrassa(17.0);
        pli.setMassaGrassaKg(15.6);
        pli.setMassaMagraKg(76.4);
        plicometriaId = repoPlicometria.save(pli).getId();

        Scheda s = new Scheda();
        s.setCliente(cliente);
        s.setNome("Scheda test PDF");
        s.setAttiva(true);
        Scheda scheda = repoScheda.save(s);
        schedaId = scheda.getId();

        AlimentoBase alimento = new AlimentoBase();
        alimento.setNome("Pasta");
        alimento.setMisuraInGrammi(100.0);
        Macro m = new Macro();
        m.setAlimento(alimento);
        m.setCalorie(350.0); m.setProteine(12.0); m.setCarboidrati(72.0); m.setGrassi(2.0);
        alimento.setMacroNutrienti(m);
        AlimentoBase alimentoSalvato = repoAlimento.save(alimento);

        Pasto pasto = new Pasto();
        pasto.setScheda(scheda);
        pasto.setNome("Pranzo");
        Pasto pastoSalvato = repoPasto.save(pasto);
        repoAlimentoPasto.save(new AlimentoPasto(alimentoSalvato, pastoSalvato, 150));
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "MISURAZIONE_ANTROPOMETRICA_READ" })
    void pdfMisurazione_primaRilevazione_200EPdfValido() throws Exception {
        assertPdfValido(mvc.perform(get("/api/misurazioni_antropometriche/{id}/pdf", misurazionePrimaId)));
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "MISURAZIONE_ANTROPOMETRICA_READ" })
    void pdfMisurazione_conConfrontoEDelta_200EPdfValido() throws Exception {
        assertPdfValido(mvc.perform(get("/api/misurazioni_antropometriche/{id}/pdf", misurazioneConDeltaId)));
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "PLICOMETRIA_READ" })
    void pdfPlicometria_200EPdfValido() throws Exception {
        assertPdfValido(mvc.perform(get("/api/plicometrie/{id}/pdf", plicometriaId)));
    }

    @Test
    @WithMockUser(username = EMAIL, authorities = { "SCHEDA_READ" })
    void pdfScheda_200EPdfValido() throws Exception {
        assertPdfValido(mvc.perform(get("/api/schede/{id}/pdf", schedaId)));
    }

    private void assertPdfValido(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        MvcResult res = result.andExpect(status().isOk()).andReturn();
        byte[] body = res.getResponse().getContentAsByteArray();
        assertThat(body.length).isGreaterThan(0);
        String header = new String(body, 0, Math.min(5, body.length), StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }
}
