package it.nutrizionista.restnutrizionista.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoRequest;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.MotivoValutazioneDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;

/**
 * Unit test (Mockito) del ramo di decisione clinica di {@link AlimentoPastoService#associaAlimento}
 * (finding D1). Verifica che:
 * <ul>
 *   <li>il blocco GRAVE ({@code ALERT_GRAVE}) sia superabile <b>solo</b> con il flag dedicato
 *       {@code confermaBloccoGrave} (NON con {@code forzaInserimento}, che resta per i WARNING);</li>
 *   <li>l'override di un blocco grave venga <b>auditato</b> ({@code recordOverrideSameTx}), mentre il
 *       superamento di un WARNING non lo sia;</li>
 *   <li>l'ownership del pasto sia verificata (403 propagato).</li>
 * </ul>
 * Stile allineato a {@code AuditServiceTest}: {@code mock()} + {@code ReflectionTestUtils.setField}
 * (nessuna strict-stubbing, quindi gli stub comuni non usati non fanno fallire il test).
 */
class AlimentoPastoServiceTest {

    private static final long PASTO_ID = 1L;
    private static final long ALIM_ID = 2L;
    private static final long CLIENTE_ID = 3L;
    private static final long SCHEDA_ID = 4L;

    private AlimentoPastoService service;
    private AlimentoPastoRepository repo;
    private PastoRepository repoPasto;
    private AlimentoBaseRepository repoAlimento;
    private ClinicalEngineService clinicalEngineService;
    private OwnershipValidator ownershipValidator;
    private AuditService auditService;

    private AlimentoBase alimento;
    private Pasto pasto;

    @BeforeEach
    void setup() {
        repo = mock(AlimentoPastoRepository.class);
        repoPasto = mock(PastoRepository.class);
        repoAlimento = mock(AlimentoBaseRepository.class);
        clinicalEngineService = mock(ClinicalEngineService.class);
        ownershipValidator = mock(OwnershipValidator.class);
        auditService = mock(AuditService.class);
        AlimentoAlternativoService alimentoAlternativoService = mock(AlimentoAlternativoService.class);

        service = new AlimentoPastoService();
        ReflectionTestUtils.setField(service, "repo", repo);
        ReflectionTestUtils.setField(service, "repoPasto", repoPasto);
        ReflectionTestUtils.setField(service, "repoAlimento", repoAlimento);
        ReflectionTestUtils.setField(service, "clinicalEngineService", clinicalEngineService);
        ReflectionTestUtils.setField(service, "ownershipValidator", ownershipValidator);
        ReflectionTestUtils.setField(service, "auditService", auditService);
        ReflectionTestUtils.setField(service, "alimentoAlternativoService", alimentoAlternativoService);

        // Grafo owned: pasto -> scheda -> cliente
        Cliente cliente = mock(Cliente.class);
        when(cliente.getId()).thenReturn(CLIENTE_ID);
        Scheda scheda = mock(Scheda.class);
        when(scheda.getCliente()).thenReturn(cliente);
        when(scheda.getId()).thenReturn(SCHEDA_ID);
        pasto = mock(Pasto.class);
        when(pasto.getScheda()).thenReturn(scheda);
        when(ownershipValidator.getOwnedPasto(PASTO_ID)).thenReturn(pasto);

        alimento = mock(AlimentoBase.class);
        when(alimento.getId()).thenReturn(ALIM_ID);
        when(alimento.getNome()).thenReturn("Pane");
        when(repoAlimento.findById(ALIM_ID)).thenReturn(Optional.of(alimento));

        when(repo.existsByPasto_IdAndAlimento_Id(PASTO_ID, ALIM_ID)).thenReturn(false);
        when(repoPasto.findByIdWithFullTree(PASTO_ID)).thenReturn(Optional.of(mock(Pasto.class)));
    }

    private AlimentoPastoRequest req(boolean forzaInserimento, boolean confermaBloccoGrave) {
        IdRequest p = mock(IdRequest.class);
        when(p.getId()).thenReturn(PASTO_ID);
        IdRequest a = mock(IdRequest.class);
        when(a.getId()).thenReturn(ALIM_ID);
        AlimentoPastoRequest r = new AlimentoPastoRequest();
        r.setPasto(p);
        r.setAlimento(a);
        r.setQuantita(100);
        r.setForzaInserimento(forzaInserimento);
        r.setConfermaBloccoGrave(confermaBloccoGrave);
        return r;
    }

    private void stubValutazione(LivelloAllerta stato, String codice, String messaggio) {
        when(clinicalEngineService.valuta(any(), any()))
                .thenReturn(new ValutazioneClinicaDto(stato, List.of(new MotivoValutazioneDto(codice, messaggio))));
    }

    @Test
    void grave_senzaConferma_bloccaEnonAudita() {
        stubValutazione(LivelloAllerta.ALERT_GRAVE, "ALL_GLUTINE", "Contiene glutine (allergene dichiarato).");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.associaAlimento(req(false, false)));
        assertTrue(ex.getMessage().contains("BLOCCO SICUREZZA"));

        verify(repo, never()).save(any());
        verify(auditService, never()).recordOverrideSameTx(any(), any(), any(), any());
    }

    @Test
    void grave_conConferma_prosegueEAudita() {
        stubValutazione(LivelloAllerta.ALERT_GRAVE, "ALL_GLUTINE", "Contiene glutine (allergene dichiarato).");

        assertDoesNotThrow(() -> service.associaAlimento(req(false, true)));

        verify(repo).save(any());
        verify(auditService).recordOverrideSameTx(eq(AuditEntityType.SCHEDA), eq(SCHEDA_ID), eq(CLIENTE_ID),
                contains("Motivi:"));
    }

    @Test
    void warning_senzaForza_blocca() {
        stubValutazione(LivelloAllerta.WARNING, "PAT_IPERTENSIONE_WARNING", "Sale oltre soglia consigliata.");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.associaAlimento(req(false, false)));
        assertTrue(ex.getMessage().contains("WARNING_RESTRIZIONE"));

        verify(repo, never()).save(any());
        verify(auditService, never()).recordOverrideSameTx(any(), any(), any(), any());
    }

    @Test
    void warning_conForza_prosegueSenzaAuditOverride() {
        stubValutazione(LivelloAllerta.WARNING, "PAT_IPERTENSIONE_WARNING", "Sale oltre soglia consigliata.");

        assertDoesNotThrow(() -> service.associaAlimento(req(true, false)));

        verify(repo).save(any());
        verify(auditService, never()).recordOverrideSameTx(any(), any(), any(), any());
    }

    @Test
    void grave_conSoloForzaInserimento_restaBloccato() {
        // Separazione dei flag (D1 punto 2): forzaInserimento NON deve superare un blocco grave.
        stubValutazione(LivelloAllerta.ALERT_GRAVE, "ALL_ARACHIDI", "Contiene arachidi (allergene dichiarato).");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.associaAlimento(req(true, false)));
        assertTrue(ex.getMessage().contains("BLOCCO SICUREZZA"));

        verify(repo, never()).save(any());
        verify(auditService, never()).recordOverrideSameTx(any(), any(), any(), any());
    }

    @Test
    void pastoAltrui_propagaForbidden() {
        when(ownershipValidator.getOwnedPasto(PASTO_ID))
                .thenThrow(new ForbiddenException("NON AUTORIZZATO: pasto non accessibile"));

        assertThrows(ForbiddenException.class, () -> service.associaAlimento(req(false, true)));

        verify(repo, never()).save(any());
        verify(auditService, never()).recordOverrideSameTx(any(), any(), any(), any());
    }
}
