package it.nutrizionista.restnutrizionista;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.security.KeycloakAdminClient;
import it.nutrizionista.restnutrizionista.service.UtenteService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * I10 — erasure CROSS-STORE (art. 17): {@code UtenteService.deleteAccount} deve cancellare l'identità Keycloak
 * <b>DOPO il commit DB</b>, SOLO se il bean {@link KeycloakAdminClient} è presente (keycloak-mode) e
 * {@code subjectId != null}. Il bean è mockato via {@code @MockitoBean} (in profilo test-legacy la config gated
 * {@code KeycloakAdminConfig} non lo crea → il mock lo rende presente, così l'{@code ObjectProvider} lo trova).
 * <p>⚠️ Il metodo di test NON è {@code @Transactional}: la tx del service committa e l'{@code afterCommit} scatta.
 * Se fosse transazionale la tx farebbe rollback → l'hook non partirebbe mai → falso verde.
 */
@SpringBootTest
@ActiveProfiles("test")
class UtenteDeleteAccountKeycloakErasureIntegrationTest extends SafeTestDatabaseBase {

    @Autowired private UtenteService utenteService;
    @Autowired private UtenteRepository repoUtente;
    @MockitoBean private KeycloakAdminClient keycloakAdminClient;

    @Test
    void deleteAccount_conSubjectId_cancellaIdentitaKeycloakDopoCommit() {
        Utente u = nuovoUtente("kc-erasure@test.it", "KCERAS00A00A0001", "SUB-KC-123");
        Long id = repoUtente.save(u).getId();

        utenteService.deleteAccount(repoUtente.findById(id).orElseThrow());

        // afterCommit già scattato: deleteAccount è ritornato dopo il commit della sua tx.
        verify(keycloakAdminClient, times(1)).deleteUser("SUB-KC-123");
    }

    @Test
    void deleteAccount_senzaSubjectId_nonChiamaKeycloak() {
        Utente u = nuovoUtente("kc-nolink@test.it", "KCERAS00A00A0002", null); // mai loggato via IdP
        Long id = repoUtente.save(u).getId();

        utenteService.deleteAccount(repoUtente.findById(id).orElseThrow());

        verifyNoInteractions(keycloakAdminClient);
    }

    private static Utente nuovoUtente(String email, String codiceFiscale, String subjectId) {
        Utente u = new Utente();
        u.setNome("KC"); u.setCognome("Erasure"); u.setCodiceFiscale(codiceFiscale);
        u.setEmail(email); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("via kc");
        u.setSubjectId(subjectId);
        return u;
    }
}
