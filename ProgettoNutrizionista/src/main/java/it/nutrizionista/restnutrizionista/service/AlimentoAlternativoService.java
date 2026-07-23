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
    private OwnershipValidator ownershipValidator;

    @Autowired
    private LimitazioneTrattamentoValidator limitazioneValidator;

    @Autowired
    private AlternativeSuggestionCalculator suggestionCalculator;

    /** A5.3 — naviga dall'alimento-pasto al cliente per il check limitazione (può essere null in test). */
    private it.nutrizionista.restnutrizionista.entity.Cliente clienteOf(AlimentoPasto ap) {
        return (ap != null && ap.getPasto() != null && ap.getPasto().getScheda() != null)
                ? ap.getPasto().getScheda().getCliente() : null;
    }

    /**
     * Crea una nuova alternativa
     */
    @Transactional
    public AlimentoAlternativoDto create(@Valid AlimentoAlternativoFormDto form) {
        AlimentoPasto alimentoPasto = ownershipValidator.getOwnedAlimentoPasto(form.getAlimentoPastoId());
        limitazioneValidator.assertNonLimitato(clienteOf(alimentoPasto)); // A5.3

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
        entity.setPasto(alimentoPasto.getPasto());
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

        // F-OWN-SWEEP: era `repo.findById` non-scoped (var `me` letta ma inutilizzata) → IDOR.
        AlimentoAlternativo entity = ownershipValidator.getOwnedAlimentoAlternativo(form.getId());
        limitazioneValidator.assertNonLimitato(clienteOf(entity.getAlimentoPasto())); // A5.3

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
        // A7-DENIED: era repo.findById... diretto (403 senza traccia d'accesso) → ora via OwnershipValidator (audit)
        AlimentoAlternativo entity = ownershipValidator.getOwnedAlimentoAlternativo(id);
        limitazioneValidator.assertNonLimitato(clienteOf(entity.getAlimentoPasto())); // A5.3
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
        // A7-DENIED: audita l'accesso negato (era repo.findById... diretto)
        return DtoMapper.toAlimentoAlternativoDto(ownershipValidator.getOwnedAlimentoAlternativo(id));
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
        limitazioneValidator.assertNonLimitato(clienteOf(alimentoPasto)); // A5.3
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
        entity.setPasto(alimentoPasto.getPasto());
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
        AlimentoPasto ownedAp = ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
        limitazioneValidator.assertNonLimitato(clienteOf(ownedAp)); // A5.3
        // A7-DENIED: audita l'accesso negato sull'alternativa (l'alimentoPastoId resta su getOwnedAlimentoPasto, sopra)
        AlimentoAlternativo entity = ownershipValidator.getOwnedAlimentoAlternativo(alternativeId);

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
        AlimentoPasto ownedAp = ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
        limitazioneValidator.assertNonLimitato(clienteOf(ownedAp)); // A5.3
        // A7-DENIED: audita l'accesso negato sull'alternativa (l'alimentoPastoId resta su getOwnedAlimentoPasto, sopra)
        AlimentoAlternativo entity = ownershipValidator.getOwnedAlimentoAlternativo(alternativeId);

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

    // === DISPLAY NAME ===

    @Transactional
    public AlimentoAlternativoDto setDisplayName(Long alternativaId, String nome) {
        AlimentoAlternativo alt = ownershipValidator.getOwnedAlimentoAlternativo(alternativaId); // A7-DENIED
        limitazioneValidator.assertNonLimitato(clienteOf(alt.getAlimentoPasto())); // A5.3
        alt.setNomeCustom(nome);
        return DtoMapper.toAlimentoAlternativoDto(repo.save(alt));
    }

    @Transactional
    public AlimentoAlternativoDto deleteDisplayName(Long alternativaId) {
        AlimentoAlternativo alt = ownershipValidator.getOwnedAlimentoAlternativo(alternativaId); // A7-DENIED
        limitazioneValidator.assertNonLimitato(clienteOf(alt.getAlimentoPasto())); // A5.3
        alt.setNomeCustom(null);
        return DtoMapper.toAlimentoAlternativoDto(repo.save(alt));
    }
}
