package it.nutrizionista.restnutrizionista.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import it.nutrizionista.restnutrizionista.dto.ConflittoClinicoDto;
import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;

/**
 * Unit test di {@link AuditService}: verifica la costruzione della riga di audit coi campi corretti
 * (attore/IP/UA catturati dal contesto) e che le letture async non rilancino se il {@code save} fallisce.
 */
class AuditServiceTest {

    private AuditService service;
    private AuditLogRepository repo;

    @BeforeEach
    void setup() {
        repo = mock(AuditLogRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Utente u = mock(Utente.class);
        when(u.getId()).thenReturn(7L);
        when(currentUserService.getMe()).thenReturn(u);

        service = new AuditService();
        ReflectionTestUtils.setField(service, "repo", repo);
        ReflectionTestUtils.setField(service, "currentUserService", currentUserService);
        ReflectionTestUtils.setField(service, "self", service); // persistAsync inline (no proxy nell'unit test)

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("mario@test.it", null, List.of()));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.5");
        req.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordCriticalNewTx_valorizzaTuttiICampi() {
        service.recordCriticalNewTx(AuditAction.SHARE, AuditEntityType.SCHEDA, 10L, 5L, "cli@x.it", AuditOutcome.SUCCESS);

        AuditLog r = captureSaved();
        assertEquals(AuditAction.SHARE, r.getAction());
        assertEquals(AuditEntityType.SCHEDA, r.getEntityType());
        assertEquals(10L, r.getEntityId());
        assertEquals(5L, r.getClienteId());
        assertEquals("cli@x.it", r.getDestinatario());
        assertEquals(AuditOutcome.SUCCESS, r.getEsito());
        assertEquals(7L, r.getUtenteId());
        assertEquals("mario@test.it", r.getUtenteEmail());
        assertEquals("203.0.113.5", r.getIpAddress());
        assertEquals("JUnit", r.getUserAgent());
    }

    @Test
    void recordDenied_usaAccessEDenied() {
        service.recordDenied(AuditEntityType.DOCUMENTO_FASCICOLO, 99L);

        AuditLog r = captureSaved();
        assertEquals(AuditAction.ACCESS, r.getAction());
        assertEquals(AuditOutcome.DENIED, r.getEsito());
        assertEquals(AuditEntityType.DOCUMENTO_FASCICOLO, r.getEntityType());
        assertEquals(99L, r.getEntityId());
    }

    @Test
    void record_nonRilancia_seIlSaveFallisce() {
        doThrow(new RuntimeException("db down")).when(repo).save(any());
        assertDoesNotThrow(() -> service.record(AuditAction.READ, AuditEntityType.CLIENTE, 1L, 1L));
    }

    @Test
    void recordOverrideGraviSameTx_unaRigaPerAlimentoIncluso() {
        var inclusi = List.of(
                new ConflittoClinicoDto(11L, "Pane", LivelloAllerta.ALERT_GRAVE, "Contiene glutine", true),
                new ConflittoClinicoDto(22L, "Salsa soia", LivelloAllerta.ALERT_GRAVE, "Contiene soia", true));

        service.recordOverrideGraviSameTx(99L, 5L, inclusi, "apply-template-pasto");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo, org.mockito.Mockito.times(2)).save(captor.capture());
        List<AuditLog> righe = captor.getAllValues();
        assertEquals(2, righe.size());
        for (AuditLog r : righe) {
            assertEquals(AuditAction.OVERRIDE_ALERT_GRAVE, r.getAction());
            assertEquals(AuditEntityType.SCHEDA, r.getEntityType());
            assertEquals(99L, r.getEntityId());
            assertEquals(5L, r.getClienteId());
            assertEquals(AuditOutcome.SUCCESS, r.getEsito());
        }
        assertEquals("Pane", extractNome(righe.get(0).getDettaglio()));
        assertEquals("Salsa soia", extractNome(righe.get(1).getDettaglio()));
    }

    @Test
    void recordOverrideGraviSameTx_listaVuota_nonScrive() {
        service.recordOverrideGraviSameTx(1L, 1L, List.of(), "ctx");
        verify(repo, org.mockito.Mockito.never()).save(any());
    }

    private static String extractNome(String dettaglio) {
        // dettaglio: "Override <ctx> — alimento '<nome>' (id=..) ..."
        int start = dettaglio.indexOf('\'') + 1;
        int end = dettaglio.indexOf('\'', start);
        return dettaglio.substring(start, end);
    }

    private AuditLog captureSaved() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(captor.capture());
        return captor.getValue();
    }
}
