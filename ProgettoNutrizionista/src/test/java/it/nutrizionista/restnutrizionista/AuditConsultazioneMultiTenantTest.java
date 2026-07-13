package it.nutrizionista.restnutrizionista;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * A7 — consultazione "storico accessi": controllo permessi (403 senza permesso), vista globale
 * dell'admin ({@code AUDIT_READ}) e isolamento multi-tenant del self-audit del nutrizionista
 * ({@code AUDIT_READ_OWN} → vede solo i propri accessi, {@code utenteId} forzato lato server).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditConsultazioneMultiTenantTest extends SafeTestDatabaseBase {

    private static final String A_EMAIL = "auditA@test.it";

    @Autowired private MockMvc mvc;
    @Autowired private UtenteRepository repoUtente;
    @Autowired private AuditLogRepository repoAudit;

    @Test
    @WithMockUser(username = "noperm@test.it", authorities = { "CLIENTE_READ" })
    void senzaPermesso_403() throws Exception {
        mvc.perform(get("/api/audit")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.it", authorities = { "AUDIT_READ" })
    void admin_vedeRigheDiPiuUtenti() throws Exception {
        seedRiga(1L, "u1@test.it");
        seedRiga(2L, "u2@test.it");

        mvc.perform(get("/api/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaleElementi").value(2));
    }

    @Test
    @WithMockUser(username = A_EMAIL, authorities = { "AUDIT_READ_OWN" })
    void nutrizionista_vedeSoloIPropriAccessi() throws Exception {
        Utente a = seedUtente(A_EMAIL);
        Long altroId = seedUtente("auditB@test.it").getId();
        seedRiga(a.getId(), A_EMAIL);
        seedRiga(a.getId(), A_EMAIL);
        seedRiga(altroId, "auditB@test.it");

        // Anche passando l'utenteId di un altro utente, /me resta limitato ai propri accessi:
        // su 3 righe totali (2 di A, 1 di B) ne devono tornare solo 2.
        mvc.perform(get("/api/audit/me").param("utenteId", altroId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaleElementi").value(2));
    }

    private Utente seedUtente(String email) {
        Utente u = new Utente();
        u.setNome("N"); u.setCognome("N");
        u.setCodiceFiscale("CF" + Math.abs(email.hashCode() % 1000000));
        u.setEmail(email); u.setPassword("x"); u.setTelefono("0"); u.setIndirizzo("x");
        return repoUtente.save(u);
    }

    private void seedRiga(Long utenteId, String email) {
        repoAudit.save(new AuditLog(utenteId, email, AuditAction.READ, AuditEntityType.CLIENTE,
                10L, 10L, AuditOutcome.SUCCESS, "127.0.0.1", "JUnit", null));
    }
}
