package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.nutrizionista.restnutrizionista.dto.ShareRequest;
import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import it.nutrizionista.restnutrizionista.service.EmailService;
import it.nutrizionista.restnutrizionista.service.FascicoloService;
import it.nutrizionista.restnutrizionista.service.OwnershipValidator;
import it.nutrizionista.restnutrizionista.service.PdfService;
import it.nutrizionista.restnutrizionista.service.SchedaService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * A7 — verifica che gli accessi ai dati sanitari generino le righe di {@link AuditLog} corrette.
 * Copre gli eventi critici sincroni (DOWNLOAD/SHARE/DELETE), l'anti-doppio-log, la riga FAILURE su
 * invio non riuscito, una lettura async (READ) e un accesso NEGATO. {@code EmailService} è mockato
 * (l'SMTP reale non è disponibile in test); l'audit async è reso deterministico dal
 * {@code SyncTaskExecutor} del profilo test (support/TestAsyncConfig).
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditIntegrationTest extends SafeTestDatabaseBase {

    private static final String OWNER = "auditOwner@test.it";
    private static final String ATTACKER = "auditAttacker@test.it";

    @Autowired private ClienteService clienteService;
    @Autowired private FascicoloService fascicoloService;
    @Autowired private OwnershipValidator ownershipValidator;
    @Autowired private UtenteRepository repoUtente;
    @Autowired private ClienteRepository repoCliente;
    @Autowired private DocumentoFascicoloRepository repoFascicolo;
    @Autowired private AuditLogRepository repoAudit;
    @Autowired private SchedaService schedaService;
    @Autowired private SchedaRepository repoScheda;

    @MockitoBean private EmailService emailService;
    @MockitoBean private PdfService pdfService;

    // ---------------------------------------------------------------- DOWNLOAD

    @Test
    @WithMockUser(username = OWNER)
    void downloadDocumento_registraUnSoloEventoDownload() throws Exception {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli1@test.it");
        DocumentoFascicolo doc = seedDocumento(cliente);

        fascicoloService.downloadDocumento(doc.getId());

        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(1, righe.size(), "download deve generare esattamente 1 riga (anti-doppio-log)");
        AuditLog r = righe.get(0);
        assertEquals(AuditAction.DOWNLOAD, r.getAction());
        assertEquals(AuditEntityType.DOCUMENTO_FASCICOLO, r.getEntityType());
        assertEquals(doc.getId(), r.getEntityId());
        assertEquals(cliente.getId(), r.getClienteId());
        assertEquals(AuditOutcome.SUCCESS, r.getEsito());
        assertEquals(OWNER, r.getUtenteEmail());
    }

    @Test
    @WithMockUser(username = OWNER)
    void downloadDocumento_seLetturaFallisce_registraFailure() {
        // Batch 4: file inesistente su disco → RuntimeException dopo la riga SUCCESS (già committata, REQUIRES_NEW).
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli7@test.it");
        DocumentoFascicolo doc = seedDocumentoFileMancante(cliente);

        assertThrows(RuntimeException.class, () -> fascicoloService.downloadDocumento(doc.getId()));

        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(2, righe.size(), "lettura fallita: 1 riga DOWNLOAD SUCCESS (pre-side-effect) + 1 riga FAILURE");
        assertTrue(righe.stream().anyMatch(r -> r.getAction() == AuditAction.DOWNLOAD && r.getEsito() == AuditOutcome.SUCCESS));
        assertTrue(righe.stream().anyMatch(r -> r.getAction() == AuditAction.DOWNLOAD && r.getEsito() == AuditOutcome.FAILURE));
    }

    // ---------------------------------------------------------------- EXPORT_PDF (rappresentativo: misurazione/plicometria = stesso wrapper)

    @Test
    @WithMockUser(username = OWNER)
    void exportPdfScheda_seGenerazioneFallisce_registraFailure() {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli8@test.it");
        Scheda scheda = new Scheda();
        scheda.setCliente(cliente);
        scheda.setNome("Scheda export");
        scheda.setAttiva(true);
        Long schedaId = repoScheda.save(scheda).getId();
        doThrow(new RuntimeException("PDF boom")).when(pdfService).generaPdfScheda(anyLong(), anyBoolean());

        assertThrows(RuntimeException.class, () -> schedaService.exportPdf(schedaId, false));

        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(2, righe.size(), "generazione PDF fallita: 1 riga EXPORT_PDF SUCCESS + 1 riga FAILURE");
        assertTrue(righe.stream().anyMatch(r -> r.getAction() == AuditAction.EXPORT_PDF && r.getEsito() == AuditOutcome.SUCCESS));
        assertTrue(righe.stream().anyMatch(r -> r.getAction() == AuditAction.EXPORT_PDF && r.getEsito() == AuditOutcome.FAILURE));
    }

    // ---------------------------------------------------------------- SHARE

    @Test
    @WithMockUser(username = OWNER)
    void shareDocumento_registraSoloShareConDestinatario() {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli2@test.it");
        DocumentoFascicolo doc = seedDocumento(cliente);

        fascicoloService.shareDocumento(doc.getId(), new ShareRequest());

        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(1, righe.size(), "share deve generare 1 sola riga SHARE (non anche DOWNLOAD)");
        AuditLog r = righe.get(0);
        assertEquals(AuditAction.SHARE, r.getAction());
        assertEquals(AuditEntityType.DOCUMENTO_FASCICOLO, r.getEntityType());
        assertEquals(AuditOutcome.SUCCESS, r.getEsito());
        assertEquals("cli2@test.it", r.getDestinatario(), "destinatario = email registrata del cliente");
    }

    @Test
    @WithMockUser(username = OWNER)
    void shareDocumento_seInvioFallisce_registraFailure() {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli3@test.it");
        DocumentoFascicolo doc = seedDocumento(cliente);
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendPdfEmail(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> fascicoloService.shareDocumento(doc.getId(), new ShareRequest()));

        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(2, righe.size(), "invio fallito: 1 riga SUCCESS (pre-invio) + 1 riga FAILURE");
        assertTrue(righe.stream().anyMatch(r -> r.getAction() == AuditAction.SHARE && r.getEsito() == AuditOutcome.SUCCESS));
        assertTrue(righe.stream().anyMatch(r -> r.getAction() == AuditAction.SHARE && r.getEsito() == AuditOutcome.FAILURE));
    }

    @Test
    @WithMockUser(username = OWNER)
    void shareDocumento_successo_valorizzaDataUltimoInvio() {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli2b@test.it");
        DocumentoFascicolo doc = seedDocumento(cliente);
        assertEquals(null, doc.getDataUltimoInvio(), "prima dell'invio il documento non risulta mai condiviso");

        fascicoloService.shareDocumento(doc.getId(), new ShareRequest());

        DocumentoFascicolo aggiornato = repoFascicolo.findById(doc.getId()).orElseThrow();
        assertNotNull(aggiornato.getDataUltimoInvio(),
                "un invio riuscito deve valorizzare dataUltimoInvio (usata dalla UI per il promemoria \"già inviato\")");
    }

    @Test
    @WithMockUser(username = OWNER)
    void shareDocumento_fallimento_nonValorizzaDataUltimoInvio() {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli3b@test.it");
        DocumentoFascicolo doc = seedDocumento(cliente);
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendPdfEmail(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> fascicoloService.shareDocumento(doc.getId(), new ShareRequest()));

        DocumentoFascicolo dopoErrore = repoFascicolo.findById(doc.getId()).orElseThrow();
        assertEquals(null, dopoErrore.getDataUltimoInvio(),
                "un invio fallito NON deve far comparire il promemoria \"già inviato\" in UI");
    }

    // ---------------------------------------------------------------- DELETE

    @Test
    @WithMockUser(username = OWNER)
    void deleteCliente_registraDeleteCheSopravviveAllaCancellazione() {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli4@test.it");
        Long clienteId = cliente.getId();

        clienteService.deleteMyCliente(clienteId);

        assertTrue(repoCliente.findById(clienteId).isEmpty(), "il cliente è cancellato");
        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(1, righe.size());
        AuditLog r = righe.get(0);
        assertEquals(AuditAction.DELETE, r.getAction());
        assertEquals(AuditEntityType.CLIENTE, r.getEntityType());
        assertEquals(clienteId, r.getClienteId(), "clienteId resta valorizzato (nessuna FK, la riga sopravvive)");
    }

    // ---------------------------------------------------------------- READ (async)

    @Test
    @WithMockUser(username = OWNER)
    void dettaglioCliente_registraRead() {
        Cliente cliente = seedCliente(seedNutrizionista(OWNER, "OWN00A00A000A"), "cli5@test.it");

        clienteService.dettaglio(cliente.getId());

        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(1, righe.size());
        assertEquals(AuditAction.READ, righe.get(0).getAction());
        assertEquals(AuditEntityType.CLIENTE, righe.get(0).getEntityType());
        assertEquals(cliente.getId(), righe.get(0).getClienteId());
    }

    // ---------------------------------------------------------------- DENIED

    @Test
    @WithMockUser(username = ATTACKER)
    void accessoNegato_registraDenied() {
        Utente owner = seedNutrizionista(OWNER, "OWN00A00A000A");
        Cliente clienteDiOwner = seedCliente(owner, "cli6@test.it");
        seedNutrizionista(ATTACKER, "ATK00A00A000A"); // l'attaccante è l'utente loggato

        assertThrows(ForbiddenException.class, () -> ownershipValidator.getOwnedCliente(clienteDiOwner.getId()));

        List<AuditLog> righe = repoAudit.findAll();
        assertEquals(1, righe.size(), "un accesso negato registra 1 riga DENIED (sopravvive al rollback)");
        AuditLog r = righe.get(0);
        assertEquals(AuditAction.ACCESS, r.getAction());
        assertEquals(AuditOutcome.DENIED, r.getEsito());
        assertEquals(AuditEntityType.CLIENTE, r.getEntityType());
        assertEquals(clienteDiOwner.getId(), r.getEntityId());
        assertNotNull(r.getUtenteEmail());
    }

    // ---------------------------------------------------------------- helpers

    private Utente seedNutrizionista(String email, String cf) {
        Utente u = new Utente();
        u.setNome("Nutri"); u.setCognome("Test"); u.setCodiceFiscale(cf);
        u.setEmail(email); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
        return repoUtente.save(u);
    }

    private Cliente seedCliente(Utente nutrizionista, String email) {
        Cliente c = new Cliente();
        c.setSesso(Sesso.Maschio);
        c.setNome("Mario"); c.setCognome("Rossi");
        c.setCodiceFiscale("MRARSS" + Math.abs(email.hashCode() % 100000) + "A");
        c.setEmail(email);
        c.setDataNascita(LocalDate.of(1990, 1, 1));
        c.setPeso(70.0); c.setAltezza(175);
        c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
        c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
        c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
        c.setBeveAlcol(false); c.setFuma(false);
        c.setNutrizionista(nutrizionista);
        return repoCliente.save(c);
    }

    private DocumentoFascicolo seedDocumento(Cliente cliente) {
        try {
            Path file = Files.createTempFile("audit-test-", ".pdf");
            Files.write(file, new byte[] { 1, 2, 3 });
            DocumentoFascicolo doc = new DocumentoFascicolo();
            doc.setCliente(cliente);
            doc.setTitolo("Documento Test");
            doc.setTipoDocumento(TipoDocumento.SCHEDA);
            doc.setPercorsoFile(file.toString());
            return repoFascicolo.save(doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Documento con file inesistente su disco: {@code Files.readAllBytes} fallisce → RuntimeException. */
    private DocumentoFascicolo seedDocumentoFileMancante(Cliente cliente) {
        DocumentoFascicolo doc = new DocumentoFascicolo();
        doc.setCliente(cliente);
        doc.setTitolo("Documento file mancante");
        doc.setTipoDocumento(TipoDocumento.SCHEDA);
        doc.setPercorsoFile("uploads/fascicoli/__inesistente__.pdf");
        return repoFascicolo.save(doc);
    }
}
