package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import jakarta.validation.Valid;

@Service
public class MisurazioneAntropometricaService {

	@Autowired private MisurazioneAntropometricaRepository repo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private OwnershipValidator ownershipValidator;
    @Autowired private LimitazioneTrattamentoValidator limitazioneValidator;
    @Autowired private GamificationService gamificationService;
    @Autowired private PdfService pdfService;
    @Autowired private AuditService auditService;
    @Autowired private SchedaFascicoloSync fascicoloSync;

    /**
     * Export PDF della misurazione per il download diretto ({@code GET /api/misurazioni_antropometriche/{id}/pdf}).
     * Registra l'audit critico EXPORT_PDF (A7) prima di generare il PDF.
     */
    @Transactional(readOnly = true)
    public byte[] exportPdf(Long id) {
        MisurazioneAntropometrica m = ownershipValidator.getOwnedMisurazioneAntropometrica(id);
        Long clienteId = m.getCliente() != null ? m.getCliente().getId() : null;
        auditService.recordCriticalNewTx(AuditAction.EXPORT_PDF, AuditEntityType.MISURAZIONE_ANTROPOMETRICA, id, clienteId, null, AuditOutcome.SUCCESS);
        try {
            return pdfService.generaPdfMisurazione(id);
        } catch (RuntimeException e) {
            // A7: side-effect fallito → riga FAILURE (il SUCCESS è già committato in REQUIRES_NEW), poi rilancio
            auditService.recordCriticalNewTx(AuditAction.EXPORT_PDF, AuditEntityType.MISURAZIONE_ANTROPOMETRICA, id, clienteId, null, AuditOutcome.FAILURE);
            throw e;
        }
    }

    @Transactional
    public MisurazioneAntropometricaDto create(@Valid MisurazioneAntropometricaFormDto form) {
        Cliente cliente = ownershipValidator.getOwnedCliente(form.getCliente().getId());
        limitazioneValidator.assertNonLimitato(cliente); // A5.3

        MisurazioneAntropometrica m = DtoMapper.toMisurazione(form);
        m.setCliente(cliente); // Associo la misurazione al cliente trovato

        MisurazioneAntropometrica salvata = repo.save(m);
        gamificationService.registraEvento(cliente.getNutrizionista(), TipoEventoGamification.MISURAZIONE_REGISTRATA, cliente.getId());
        fascicoloSync.sincronizza(cliente.getId(), TipoDocumento.MISURAZIONE, salvata.getId());
        return DtoMapper.toMisurazioneDtoLight(salvata);
    }


	@Transactional
	public MisurazioneAntropometricaDto update(@Valid MisurazioneAntropometricaFormDto form) {
		if (form.getId() == null) throw new BadRequestException("Id Misurazione obbligatoria per update");
		MisurazioneAntropometrica m = ownershipValidator.getOwnedMisurazioneAntropometrica(form.getId());
		limitazioneValidator.assertNonLimitato(m.getCliente()); // A5.3
		DtoMapper.updateMisurazioneFromForm(m, form);
		MisurazioneAntropometrica salvata = repo.save(m);
		fascicoloSync.sincronizza(salvata.getCliente().getId(), TipoDocumento.MISURAZIONE, salvata.getId());
		return DtoMapper.toMisurazioneDtoLight(salvata);
	}

	@Transactional
    public void delete(Long id) {
        MisurazioneAntropometrica m = ownershipValidator.getOwnedMisurazioneAntropometrica(id);
        limitazioneValidator.assertNonLimitato(m.getCliente()); // A5.3
        Long clienteId = m.getCliente() != null ? m.getCliente().getId() : null;
        repo.deleteById(id);
        if (clienteId != null) fascicoloSync.elimina(clienteId, TipoDocumento.MISURAZIONE, id);
    }

	@Transactional(readOnly = true)
	public PageResponse<MisurazioneAntropometricaDto> allMisurazioniCliente(Long id,Pageable pageable) {
        ownershipValidator.getOwnedCliente(id);
		auditService.record(AuditAction.LIST, AuditEntityType.MISURAZIONE_ANTROPOMETRICA, null, id);
		return PageResponse.from(repo.findByCliente_IdOrderByDataMisurazioneDesc(id,pageable).map(DtoMapper::toMisurazioneDtoLight));
	}

}
