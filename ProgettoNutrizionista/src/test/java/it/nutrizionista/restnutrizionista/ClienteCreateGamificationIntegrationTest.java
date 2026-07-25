package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.EventoGamificationRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Regression del fix "gamification afterCommit": dopo {@link ClienteService#create} l'evento
 * {@code NUOVO_CLIENTE} viene registrato. La registrazione è deferita a
 * {@code TransactionSynchronization.afterCommit} (in {@code GamificationService.registraEvento}) per
 * evitare un self-deadlock TiDB: su TiDB (&lt; v8.5.6) l'INSERT di una figlia con FK prende un lock
 * ESCLUSIVO sulla riga parent, quindi l'insert gamification (FK-&gt;utenti) in una {@code REQUIRES_NEW}
 * annidata nella tx di {@code create} (che ha già inserito {@code clienti}, FK-&gt;utenti) andrebbe in
 * self-deadlock (50s / SQL 1205); a commit avvenuto il lock è rilasciato.
 *
 * <p>⚠️ NON-{@code @Transactional} (via {@link SafeTestDatabaseBase}): una tx di test non committerebbe
 * mai e l'{@code afterCommit} non scatterebbe (falso verde). L'{@code afterCommit} gira sincrono sullo
 * stesso thread durante il commit di {@code create}, quindi l'evento è già persistito quando
 * {@code create} ritorna.
 *
 * <p>⚠️ LIMITE: su H2 il deadlock TiDB NON si riproduce → questo test prova il <b>path afterCommit</b>
 * (evento registrato dopo il commit, {@code Utente} detached-safe), NON la rimozione del deadlock —
 * quella si verifica LIVE su TiDB (create rapido, non 50s + evento presente).
 */
@SpringBootTest
@ActiveProfiles("test")
class ClienteCreateGamificationIntegrationTest extends SafeTestDatabaseBase {

    private static final String EMAIL = "nutri.gami@test.it";

    @Autowired private ClienteService clienteService;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private UtenteRepository utenteRepo;
    @Autowired private EventoGamificationRepository eventoRepo;

    private Long nutrizionistaId;

    @BeforeEach
    void seed() {
        Utente u = new Utente();
        u.setNome("Nutri"); u.setCognome("Gami"); u.setCodiceFiscale("GMINRZ00A00A000A");
        u.setEmail(EMAIL); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
        nutrizionistaId = utenteRepo.save(u).getId();
    }

    private ClienteFormDto form() {
        ClienteFormDto f = new ClienteFormDto();
        f.setSesso(Sesso.Maschio);
        f.setNome("Mario");
        f.setCognome("Rossi");
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
    void create_registraEventoNuovoCliente_dopoIlCommit() {
        ClienteDto dto = clienteService.create(form());

        // Cliente persistito: la tx di create è committata → l'afterCommit è già scattato (stesso thread).
        assertThat(clienteRepo.findById(dto.getId())).isPresent();

        // Evento NUOVO_CLIENTE registrato via afterCommit (oggi su TiDB andava perso per il deadlock).
        boolean eventoRegistrato = eventoRepo.existsByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                nutrizionistaId, TipoEventoGamification.NUOVO_CLIENTE, Instant.EPOCH);
        assertThat(eventoRegistrato)
                .as("l'evento NUOVO_CLIENTE deve essere registrato via afterCommit dopo create")
                .isTrue();
    }
}
