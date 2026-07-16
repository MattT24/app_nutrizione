package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * CF/email del cliente FACOLTATIVI: un cliente si crea senza CF né email; più clienti senza CF/email
 * non collidono (dedup null/blank-safe + normalizzazione ""→null); il dedup resta attivo quando valorizzati.
 */
@SpringBootTest
@ActiveProfiles("test")
class ClienteCfEmailOpzionaliTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri.cfopz@test.it";

    @Autowired private ClienteService clienteService;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private UtenteRepository utenteRepo;

    @BeforeEach
    void seed() {
        Utente u = new Utente();
        u.setNome("Nutri"); u.setCognome("CfOpz"); u.setCodiceFiscale("CFOAAA00A00A000A");
        u.setEmail(EMAIL); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
        utenteRepo.save(u);
    }

    private ClienteFormDto form(String nome, String cf, String email) {
        ClienteFormDto f = new ClienteFormDto();
        f.setSesso(Sesso.Maschio);
        f.setNome(nome);
        f.setCognome("Rossi");
        f.setCodiceFiscale(cf);
        f.setEmail(email);
        // Campi testo NOT NULL nell'entity (il form reale li invia come "" di default).
        f.setIntolleranze("");
        f.setFunzioniIntestinali("");
        f.setProblematicheSalutari("");
        f.setQuantitaEQualitaDelSonno("");
        f.setAssunzioneFarmaci("");
        f.setBeveAlcol(false);
        f.setFuma(false);
        return f;
    }

    @Test
    @WithMockUser(username = EMAIL)
    void create_senzaCfNeEmail_persisteConNull() {
        // Stringhe vuote (come le invia il form) → normalizzate a null.
        ClienteDto dto = clienteService.create(form("Mario", "", ""));
        Cliente saved = clienteRepo.findById(dto.getId()).orElseThrow();
        assertThat(saved.getCodiceFiscale()).isNull();
        assertThat(saved.getEmail()).isNull();
    }

    @Test
    @WithMockUser(username = EMAIL)
    void due_clienti_senzaCfEmail_nonCollidono() {
        clienteService.create(form("Mario", "", ""));
        // Il secondo cliente senza CF/email NON deve dare 409 (dedup saltato su blank/null).
        assertThatCode(() -> clienteService.create(form("Luigi", null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(username = EMAIL)
    void dedup_restaAttivo_seEmailValorizzataEDuplicata() {
        clienteService.create(form("Mario", null, "mario@x.it"));
        assertThatThrownBy(() -> clienteService.create(form("Luigi", null, "mario@x.it")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @WithMockUser(username = EMAIL)
    void dedup_restaAttivo_seCfValorizzatoEDuplicato() {
        clienteService.create(form("Mario", "RSSMRA80A01H501U", null));
        assertThatThrownBy(() -> clienteService.create(form("Luigi", "RSSMRA80A01H501U", null)))
                .isInstanceOf(ConflictException.class);
    }
}
