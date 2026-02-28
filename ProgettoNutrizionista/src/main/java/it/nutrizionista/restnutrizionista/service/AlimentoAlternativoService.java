package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoFormDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoUpsertDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import jakarta.validation.Valid;

/**
 * Service per la gestione degli alimenti alternativi
 */
@Service
public class AlimentoAlternativoService {

    @Autowired
    private AlimentoAlternativoRepository repo;

    @Autowired
    private AlimentoPastoRepository alimentoPastoRepo;

    @Autowired
    private AlimentoBaseRepository alimentoBaseRepo;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private OwnershipValidator ownershipValidator;

    @Autowired
    private AlternativeSuggestionCalculator suggestionCalculator;

    /**
     * Crea una nuova alternativa
     */
    @Transactional
    public AlimentoAlternativoDto create(@Valid AlimentoAlternativoFormDto form) {
        AlimentoPasto alimentoPasto = ownershipValidator.getOwnedAlimentoPasto(form.getAlimentoPastoId());

        // Verifica che l'alimento alternativo esista
        AlimentoBase alimentoAlt = alimentoBaseRepo.findById(form.getAlimentoAlternativoId())
                .orElseThrow(() -> new NotFoundException(
                        "Alimento alternativo non trovato con id: " + form.getAlimentoAlternativoId()));

        // Verifica che l'alimento alternativo sia diverso dall'alimento principale
        if (alimentoPasto.getAlimento() != null &&
                alimentoPasto.getAlimento().getId().equals(form.getAlimentoAlternativoId())) {
            throw new BadRequestException("L'alimento alternativo deve essere diverso dall'alimento principale");
        }

        // Verifica duplicati
        if (repo.existsByAlimentoPasto_IdAndAlimentoAlternativo_Id(
                form.getAlimentoPastoId(), form.getAlimentoAlternativoId())) {
            throw new ConflictException("Questa alternativa esiste già per questo alimento nel pasto");
        }

        // Calcola la prossima priorità se non specificata
        Integer priorita = form.getPriorita();
        if (priorita == null || priorita < 1) {
            priorita = (int) repo.countByAlimentoPasto_Id(form.getAlimentoPastoId()) + 1;
        }

        AlternativeMode mode = form.getMode() != null ? form.getMode() : AlternativeMode.CALORIE;
        boolean manual = Boolean.TRUE.equals(form.getManual());
        Integer quantita = form.getQuantita();
        if (!manual && quantita == null) {
            quantita = suggestionCalculator.suggestQuantity(alimentoPasto, alimentoAlt, mode);
        }

        AlimentoAlternativo entity = new AlimentoAlternativo();
        entity.setAlimentoPasto(alimentoPasto);
        entity.setAlimentoAlternativo(alimentoAlt);
        entity.setQuantita(quantita != null ? quantita : 100);
        entity.setPriorita(priorita);
        entity.setMode(mode);
        entity.setManual(manual);
        entity.setNote(form.getNote());

        return DtoMapper.toAlimentoAlternativoDto(repo.save(entity));
    }

    /**
     * Aggiorna un'alternativa esistente (quantità, priorità, note)
     */
    @Transactional
    public AlimentoAlternativoDto update(@Valid AlimentoAlternativoFormDto form) {
        if (form.getId() == null) {
            throw new BadRequestException("ID obbligatorio per l'aggiornamento");
        }

        var me = currentUserService.getMe();
        AlimentoAlternativo entity = repo.findById(form.getId())
                .orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alternativa non accessibile"));

        // Aggiorna solo i campi modificabili
        if (form.getQuantita() != null) {
            entity.setQuantita(form.getQuantita());
        }
        if (form.getPriorita() != null) {
            entity.setPriorita(form.getPriorita());
        }
        if (form.getMode() != null) {
            entity.setMode(form.getMode());
        }
        if (form.getManual() != null) {
            entity.setManual(form.getManual());
        }
        entity.setNote(form.getNote());

        if (Boolean.FALSE.equals(entity.getManual()) && form.getQuantita() == null) {
            entity.setQuantita(suggestionCalculator.suggestQuantity(entity.getAlimentoPasto(),
                    entity.getAlimentoAlternativo(), entity.getMode()));
        }

        return DtoMapper.toAlimentoAlternativoDto(repo.save(entity));
    }

    /**
     * Elimina un'alternativa per ID
     */
    @Transactional
    public void delete(Long id) {
        var me = currentUserService.getMe();
        AlimentoAlternativo entity = repo.findByIdAndAlimentoPasto_Pasto_Scheda_Cliente_Nutrizionista_Id(id, me.getId())
                .orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alternativa non accessibile"));
        repo.delete(entity);
    }

    /**
     * Lista tutte le alternative per un alimento in pasto, ordinate per priorità
     */
    @Transactional(readOnly = true)
    public List<AlimentoAlternativoDto> listByAlimentoPasto(Long alimentoPastoId) {
        ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
        return repo.findByAlimentoPasto_IdOrderByPrioritaAsc(alimentoPastoId).stream()
                .map(DtoMapper::toAlimentoAlternativoDto)
                .collect(Collectors.toList());
    }

    /**
     * Ottiene una singola alternativa per ID
     */
    @Transactional(readOnly = true)
    public AlimentoAlternativoDto getById(Long id) {
        var me = currentUserService.getMe();
        return repo.findByIdAndAlimentoPasto_Pasto_Scheda_Cliente_Nutrizionista_Id(id, me.getId())
                .map(DtoMapper::toAlimentoAlternativoDto)
                .orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alternativa non accessibile"));
    }

    @Transactional
    public void recomputeAutoAlternativesForAlimentoPasto(AlimentoPasto alimentoPasto) {
        if (alimentoPasto == null)
            return;
        List<AlimentoAlternativo> alternatives = repo.findByAlimentoPasto_IdOrderByPrioritaAsc(alimentoPasto.getId());
        boolean anyChanged = false;
        for (AlimentoAlternativo alt : alternatives) {
            if (Boolean.TRUE.equals(alt.getManual()))
                continue;
            Integer suggested = suggestionCalculator.suggestQuantity(alimentoPasto, alt.getAlimentoAlternativo(),
                    alt.getMode());
            if (suggested != null && !suggested.equals(alt.getQuantita())) {
                alt.setQuantita(suggested);
                anyChanged = true;
            }
        }
        if (anyChanged) {
            repo.saveAll(alternatives);
        }
    }

    @Transactional
    public AlimentoAlternativoDto createForAlimentoPasto(Long alimentoPastoId,
            @Valid AlimentoAlternativoUpsertDto body) {
        AlimentoPasto alimentoPasto = ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
        AlimentoBase alimentoAlt = alimentoBaseRepo.findById(body.getAlimentoAlternativoId())
                .orElseThrow(() -> new NotFoundException(
                        "Alimento alternativo non trovato con id: " + body.getAlimentoAlternativoId()));

        if (alimentoPasto.getAlimento() != null &&
                alimentoPasto.getAlimento().getId().equals(body.getAlimentoAlternativoId())) {
            throw new BadRequestException("L'alimento alternativo deve essere diverso dall'alimento principale");
        }

        if (repo.existsByAlimentoPasto_IdAndAlimentoAlternativo_Id(alimentoPastoId, body.getAlimentoAlternativoId())) {
            throw new ConflictException("Questa alternativa esiste già per questo alimento nel pasto");
        }

        Integer priorita = body.getPriorita();
        if (priorita == null || priorita < 1) {
            priorita = (int) repo.countByAlimentoPasto_Id(alimentoPastoId) + 1;
        }

        AlternativeMode mode = body.getMode() != null ? body.getMode() : AlternativeMode.CALORIE;
        boolean manual = Boolean.TRUE.equals(body.getManual());

        Integer quantita = body.getQuantita();
        if (!manual && quantita == null) {
            quantita = suggestionCalculator.suggestQuantity(alimentoPasto, alimentoAlt, mode);
        }

        AlimentoAlternativo entity = new AlimentoAlternativo();
        entity.setAlimentoPasto(alimentoPasto);
        entity.setAlimentoAlternativo(alimentoAlt);
        entity.setQuantita(quantita != null ? quantita : 100);
        entity.setPriorita(priorita);
        entity.setMode(mode);
        entity.setManual(manual);
        entity.setNote(body.getNote());

        return DtoMapper.toAlimentoAlternativoDto(repo.save(entity));
    }

    @Transactional
    public AlimentoAlternativoDto updateForAlimentoPasto(Long alimentoPastoId, Long alternativeId,
            @Valid AlimentoAlternativoUpsertDto body) {
        ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
        var me = currentUserService.getMe();

        AlimentoAlternativo entity = repo
                .findByIdAndAlimentoPasto_Pasto_Scheda_Cliente_Nutrizionista_Id(alternativeId, me.getId())
                .orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alternativa non accessibile"));

        if (entity.getAlimentoPasto() == null || !entity.getAlimentoPasto().getId().equals(alimentoPastoId)) {
            throw new BadRequestException("Alternativa non associata all'alimento nel pasto indicato");
        }

        if (body.getQuantita() != null) {
            entity.setQuantita(body.getQuantita());
        }
        if (body.getPriorita() != null) {
            entity.setPriorita(body.getPriorita());
        }
        if (body.getMode() != null) {
            entity.setMode(body.getMode());
        }
        if (body.getManual() != null) {
            entity.setManual(body.getManual());
        }
        if (body.getNote() != null) {
            entity.setNote(body.getNote());
        }

        if (Boolean.FALSE.equals(entity.getManual()) && body.getQuantita() == null) {
            entity.setQuantita(suggestionCalculator.suggestQuantity(entity.getAlimentoPasto(),
                    entity.getAlimentoAlternativo(), entity.getMode()));
        }

        return DtoMapper.toAlimentoAlternativoDto(repo.save(entity));
    }

    @Transactional
    public void deleteForAlimentoPasto(Long alimentoPastoId, Long alternativeId) {
        ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
        var me = currentUserService.getMe();

        AlimentoAlternativo entity = repo
                .findByIdAndAlimentoPasto_Pasto_Scheda_Cliente_Nutrizionista_Id(alternativeId, me.getId())
                .orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alternativa non accessibile"));

        if (entity.getAlimentoPasto() == null || !entity.getAlimentoPasto().getId().equals(alimentoPastoId)) {
            throw new BadRequestException("Alternativa non associata all'alimento nel pasto indicato");
        }

        repo.delete(entity);
    }

    @Transactional
    public List<AlimentoAlternativoDto> bulkUpsertForAlimentoPasto(Long alimentoPastoId,
            @Valid List<AlimentoAlternativoUpsertDto> items) {
        if (items == null)
            return listByAlimentoPasto(alimentoPastoId);

        List<AlimentoAlternativoDto> result = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            AlimentoAlternativoUpsertDto item = items.get(i);
            if (item == null)
                continue;
            if (item.getPriorita() == null || item.getPriorita() < 1) {
                item.setPriorita(i + 1);
            }
            if (item.getId() == null) {
                result.add(createForAlimentoPasto(alimentoPastoId, item));
            } else {
                result.add(updateForAlimentoPasto(alimentoPastoId, item.getId(), item));
            }
        }
        return result;
    }

    // === PER-PASTO METHODS ===

    @Transactional(readOnly = true)
    public List<AlimentoAlternativoDto> listByPasto(Long pastoId) {
        ownershipValidator.getOwnedPasto(pastoId);
        return repo.findByPasto_IdOrderByPrioritaAsc(pastoId).stream()
                .map(DtoMapper::toAlimentoAlternativoDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlimentoAlternativoDto createForPasto(Long pastoId,
            @Valid AlimentoAlternativoUpsertDto body) {
        var pasto = ownershipValidator.getOwnedPasto(pastoId);

        AlimentoBase alimentoAlt = alimentoBaseRepo.findById(body.getAlimentoAlternativoId())
                .orElseThrow(() -> new NotFoundException(
                        "Alimento alternativo non trovato con id: " + body.getAlimentoAlternativoId()));

        if (repo.existsByPasto_IdAndAlimentoAlternativo_Id(pastoId, body.getAlimentoAlternativoId())) {
            throw new ConflictException("Questa alternativa esiste già per questo pasto");
        }

        Integer priorita = body.getPriorita();
        if (priorita == null || priorita < 1) {
            priorita = (int) repo.countByPasto_Id(pastoId) + 1;
        }

        AlternativeMode mode = body.getMode() != null ? body.getMode() : AlternativeMode.CALORIE;
        boolean manual = Boolean.TRUE.equals(body.getManual());
        Integer quantita = body.getQuantita();
        if (!manual && quantita == null) {
            // Use first food in meal as reference for auto-suggest
            var alimentiPasto = pasto.getAlimentiPasto();
            if (alimentiPasto != null && !alimentiPasto.isEmpty()) {
                quantita = suggestionCalculator.suggestQuantity(alimentiPasto.iterator().next(), alimentoAlt, mode);
            }
        }

        AlimentoAlternativo entity = new AlimentoAlternativo();
        entity.setPasto(pasto);
        entity.setAlimentoAlternativo(alimentoAlt);
        entity.setQuantita(quantita != null ? quantita : 100);
        entity.setPriorita(priorita);
        entity.setMode(mode);
        entity.setManual(manual);
        entity.setNote(body.getNote());

        return DtoMapper.toAlimentoAlternativoDto(repo.save(entity));
    }

    @Transactional
    public AlimentoAlternativoDto updateForPasto(Long pastoId, Long alternativeId,
            @Valid AlimentoAlternativoUpsertDto body) {
        ownershipValidator.getOwnedPasto(pastoId);

        AlimentoAlternativo entity = repo.findById(alternativeId)
                .orElseThrow(() -> new NotFoundException("Alternativa non trovata con id: " + alternativeId));

        if (entity.getPasto() == null || !entity.getPasto().getId().equals(pastoId)) {
            throw new BadRequestException("Alternativa non associata al pasto indicato");
        }

        if (body.getQuantita() != null) entity.setQuantita(body.getQuantita());
        if (body.getPriorita() != null) entity.setPriorita(body.getPriorita());
        if (body.getMode() != null) entity.setMode(body.getMode());
        if (body.getManual() != null) entity.setManual(body.getManual());
        if (body.getNote() != null) entity.setNote(body.getNote());

        return DtoMapper.toAlimentoAlternativoDto(repo.save(entity));
    }

    @Transactional
    public void deleteForPasto(Long pastoId, Long alternativeId) {
        ownershipValidator.getOwnedPasto(pastoId);

        AlimentoAlternativo entity = repo.findById(alternativeId)
                .orElseThrow(() -> new NotFoundException("Alternativa non trovata con id: " + alternativeId));

        if (entity.getPasto() == null || !entity.getPasto().getId().equals(pastoId)) {
            throw new BadRequestException("Alternativa non associata al pasto indicato");
        }

        repo.delete(entity);
    }

    // === BATCH PER-SCHEDA ===

    @Transactional(readOnly = true)
    public Map<Long, List<AlimentoAlternativoDto>> listByScheda(Long schedaId) {
        ownershipValidator.getOwnedScheda(schedaId);
        return repo.findByPasto_Scheda_IdOrderByPastoIdAndPrioritaAsc(schedaId).stream()
                .map(DtoMapper::toAlimentoAlternativoDto)
                .collect(Collectors.groupingBy(dto -> dto.getPastoId()));
    }
}
