package it.nutrizionista.restnutrizionista.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import it.nutrizionista.restnutrizionista.dto.CopyBulkRequest;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;

/**
 * Regressione di SCOPE (finding F-D1a, nota di review): la duplicazione INTRA-paziente non deve
 * eseguire alcun re-check clinico (decisione "niente attrito intra-paziente"); la duplicazione
 * CROSS-paziente invece deve valutare via MDSS. Blindato con Mockito (verify never()/times()).
 */
class CopyBulkScopeRegressionTest {

    private static final long SOURCE = 1L;
    private static final long TARGET = 2L;
    private static final long SCHEDA_ID = 10L;

    private SchedaService service;
    private SchedaRepository repo;
    private OwnershipValidator ownershipValidator;
    private ClinicalEngineService clinicalEngineService;
    private AuditService auditService;

    private Cliente source;
    private Scheda originale;

    @BeforeEach
    void setup() {
        repo = mock(SchedaRepository.class);
        ownershipValidator = mock(OwnershipValidator.class);
        clinicalEngineService = mock(ClinicalEngineService.class);
        auditService = mock(AuditService.class);

        service = new SchedaService();
        ReflectionTestUtils.setField(service, "repo", repo);
        ReflectionTestUtils.setField(service, "ownershipValidator", ownershipValidator);
        ReflectionTestUtils.setField(service, "clinicalEngineService", clinicalEngineService);
        ReflectionTestUtils.setField(service, "auditService", auditService);

        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        source = mock(Cliente.class);
        when(source.getId()).thenReturn(SOURCE);
        when(source.getNome()).thenReturn("Mario");
        when(source.getCognome()).thenReturn("Rossi");

        // Scheda sorgente con un alimento (così una CROSS chiamerebbe davvero il motore).
        AlimentoBase alimento = mock(AlimentoBase.class);
        when(alimento.getId()).thenReturn(99L);
        when(alimento.getNome()).thenReturn("Pane");
        Pasto pasto = new Pasto();
        pasto.setNome("Colazione");
        AlimentoPasto ap = new AlimentoPasto(alimento, pasto, 100); // id null (non persistito)
        pasto.setAlimentiPasto(new LinkedHashSet<>(Set.of(ap)));

        originale = new Scheda();
        originale.setCliente(source);
        originale.setNome("Dieta base");
        originale.setPasti(new LinkedHashSet<>(Set.of(pasto)));

        when(ownershipValidator.getOwnedSchedaFullDetails(SCHEDA_ID)).thenReturn(originale);
    }

    private CopyBulkRequest req(long targetId, boolean force) {
        CopyBulkRequest r = new CopyBulkRequest();
        r.setTargetClienteIds(List.of(targetId));
        r.setForce(force);
        return r;
    }

    @Test
    void intraPaziente_nonEsegueAlcunCheckClinico() {
        when(ownershipValidator.getOwnedCliente(SOURCE)).thenReturn(source);

        service.copyBulk(SCHEDA_ID, req(SOURCE, false));

        // Duplicazione stesso paziente → NESSUNA valutazione clinica, NESSUN audit override.
        verify(clinicalEngineService, never()).conflittiClinici(any(), any());
        verify(clinicalEngineService, never()).valutaInBatch(any(), any());
        verify(auditService, never()).recordOverrideGraviSameTx(anyLong(), anyLong(), any(), any());
    }

    @Test
    void crossPaziente_esegueIlCheckClinico() {
        Cliente target = mock(Cliente.class);
        when(target.getId()).thenReturn(TARGET);
        when(target.getNome()).thenReturn("Luigi");
        when(target.getCognome()).thenReturn("Verdi");
        when(ownershipValidator.getOwnedCliente(TARGET)).thenReturn(target);
        // Nessun conflitto → prosegue senza audit; ma il motore DEVE essere interrogato.
        when(clinicalEngineService.conflittiClinici(any(), any())).thenReturn(List.of());

        service.copyBulk(SCHEDA_ID, req(TARGET, false));

        verify(clinicalEngineService, times(1)).conflittiClinici(any(), any());
    }
}
