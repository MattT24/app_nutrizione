package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PlicometriaDto;
import it.nutrizionista.restnutrizionista.dto.PlicometriaFormDto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import jakarta.validation.Valid;

@Service
public class PlicometriaService {
	
	@Autowired private PlicometriaCalcoliService calcoliService;

    @Autowired private PlicometriaRepository repo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private OwnershipValidator ownershipValidator;
    @Autowired private LimitazioneTrattamentoValidator limitazioneValidator;
    @Autowired private PdfService pdfService;
    @Autowired private AuditService auditService;

    /**
     * Export PDF della plicometria per il download diretto ({@code GET /api/plicometrie/{id}/pdf}).
     * Registra l'audit critico EXPORT_PDF (A7) prima di generare il PDF.
     */
    @Transactional(readOnly = true)
    public byte[] exportPdf(Long id) {
        Plicometria p = ownershipValidator.getOwnedPlicometria(id);
        Long clienteId = p.getCliente() != null ? p.getCliente().getId() : null;
        auditService.recordCriticalNewTx(AuditAction.EXPORT_PDF, AuditEntityType.PLICOMETRIA, id, clienteId, null, AuditOutcome.SUCCESS);
        try {
            return pdfService.generaPdfPlicometria(id);
        } catch (RuntimeException e) {
            // A7: side-effect fallito → riga FAILURE (il SUCCESS è già committato in REQUIRES_NEW), poi rilancio
            auditService.recordCriticalNewTx(AuditAction.EXPORT_PDF, AuditEntityType.PLICOMETRIA, id, clienteId, null, AuditOutcome.FAILURE);
            throw e;
        }
    }

    @Transactional
    public PlicometriaDto create(@Valid PlicometriaFormDto form) {
        Cliente cliente = ownershipValidator.getOwnedCliente(form.getCliente().getId());
        limitazioneValidator.assertNonLimitato(cliente); // A5.3

        // Conversione DTO -> Entity
        Plicometria p = DtoMapper.toPlicometria(form);
        p.setCliente(cliente); // Associo la plicometria al cliente
        var res = calcoliService.calcola(p, cliente);

        p.setPesoKgRiferimento(cliente.getPeso());
        p.setSommaPliche(res.sommaPliche());
        p.setDensitaCorporea(res.densitaCorporea());
        p.setPercentualeMassaGrassa(res.percentualeMassaGrassa());
        p.setMassaGrassaKg(res.massaGrassaKg());
        p.setMassaMagraKg(res.massaMagraKg());

        // Nota: Il calcolo della % massa grassa potrebbe essere fatto qui se non arriva dal FE
        // es: p.setPercentualeMassaGrassa(CalcoliService.calcola(p));

        return DtoMapper.toPlicometriaDto(repo.save(p));
    }

    @Transactional
    public PlicometriaDto update(@Valid PlicometriaFormDto form) {
        if (form.getId() == null) {
            throw new BadRequestException("Id Plicometria obbligatorio per update");
        }

        Plicometria p = ownershipValidator.getOwnedPlicometria(form.getId());
        limitazioneValidator.assertNonLimitato(p.getCliente()); // A5.3

        // Aggiorno i campi usando il Mapper
        DtoMapper.updatePlicometriaFromForm(p, form);
        
        var res = calcoliService.calcola(p, p.getCliente());

        p.setPesoKgRiferimento(p.getCliente().getPeso());
        p.setSommaPliche(res.sommaPliche());
        p.setDensitaCorporea(res.densitaCorporea());
        p.setPercentualeMassaGrassa(res.percentualeMassaGrassa());
        p.setMassaGrassaKg(res.massaGrassaKg());
        p.setMassaMagraKg(res.massaMagraKg());

        
        return DtoMapper.toPlicometriaDto(repo.save(p));
    }

    @Transactional
    public void delete(Long id) {
        Plicometria p = ownershipValidator.getOwnedPlicometria(id);
        limitazioneValidator.assertNonLimitato(p.getCliente()); // A5.3

        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlicometriaDto> allPlicometrieByCliente(Long clienteId, Pageable pageable) {
        ownershipValidator.getOwnedCliente(clienteId);
        auditService.record(AuditAction.LIST, AuditEntityType.PLICOMETRIA, null, clienteId);

        // Recupero paginato ordinato per data decrescente
        return PageResponse.from(
            repo.findByCliente_IdOrderByDataMisurazioneDesc(clienteId, pageable)
                .map(DtoMapper::toPlicometriaDto)
        );
    }
}
