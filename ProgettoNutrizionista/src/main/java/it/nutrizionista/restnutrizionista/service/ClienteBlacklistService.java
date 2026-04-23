package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AvversionePersonaleDto;
import it.nutrizionista.restnutrizionista.dto.AvversionePersonaleFormDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AvversionePersonaleRepository;

/**
 * Servizio dedicato alla gestione CRUD della Blacklist (AvversionePersonale)
 * per un singolo Cliente. Utilizza OwnershipValidator per la protezione IDOR.
 */
@Service
public class ClienteBlacklistService {

    private final OwnershipValidator ownershipValidator;
    private final AvversionePersonaleRepository avversioneRepository;
    private final AlimentoBaseRepository alimentoBaseRepository;

    // ── Iniezione Esplicita via Costruttore (NO LOMBOK) ──
    public ClienteBlacklistService(
            OwnershipValidator ownershipValidator,
            AvversionePersonaleRepository avversioneRepository,
            AlimentoBaseRepository alimentoBaseRepository) {
        this.ownershipValidator = ownershipValidator;
        this.avversioneRepository = avversioneRepository;
        this.alimentoBaseRepository = alimentoBaseRepository;
    }

    /**
     * Recupera la lista delle avversioni alimentari per il cliente specificato.
     * La query JOIN FETCH a.alimento previene LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<AvversionePersonaleDto> getBlacklistByCliente(Long clienteId) {
        // IDOR Gate: verifica ownership prima di qualsiasi operazione
        ownershipValidator.getOwnedCliente(clienteId);

        return avversioneRepository.findByClienteIdWithAlimenti(clienteId)
                .stream()
                .map(DtoMapper::toAvversionePersonaleDto)
                .collect(Collectors.toList());
    }

    /**
     * Aggiunge un alimento alla blacklist del cliente.
     * Anti-Duplication: verifica che l'alimento non sia già presente (UniqueConstraint).
     * Persistenza Diretta: istanzia esplicitamente l'entity senza cascade abuse.
     */
    @Transactional
    public AvversionePersonaleDto addAlimentoToBlacklist(Long clienteId, AvversionePersonaleFormDto form) {
        // IDOR Gate
        Cliente cliente = ownershipValidator.getOwnedCliente(clienteId);

        // Estrazione validata dell'alimento
        AlimentoBase alimento = alimentoBaseRepository.findById(form.alimentoId())
                .orElseThrow(() -> new NotFoundException("Alimento con ID " + form.alimentoId() + " non trovato."));

        // Anti-Duplication Rule: impedisce inserimenti duplicati
        avversioneRepository.findByClienteIdAndAlimentoId(clienteId, form.alimentoId())
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "L'alimento '" + alimento.getNome() + "' è già presente nella blacklist del paziente.");
                });

        // Persistenza diretta (O(1) memory footprint, no cascade abuse)
        AvversionePersonale avversione = new AvversionePersonale(cliente, alimento, form.gravita(), form.note());
        AvversionePersonale saved = avversioneRepository.save(avversione);

        return DtoMapper.toAvversionePersonaleDto(saved);
    }

    /**
     * Rimuove un alimento dalla blacklist del cliente tramite alimentoId.
     * Path param semantico: usiamo l'ID dell'alimento, non l'ID della riga pivot.
     */
    @Transactional
    public void removeAlimentoFromBlacklist(Long clienteId, Long alimentoId) {
        // IDOR Gate
        ownershipValidator.getOwnedCliente(clienteId);

        AvversionePersonale avversione = avversioneRepository.findByClienteIdAndAlimentoId(clienteId, alimentoId)
                .orElseThrow(() -> new NotFoundException(
                        "Avversione per l'alimento con ID " + alimentoId + " non trovata per questo paziente."));

        avversioneRepository.delete(avversione);
    }
}
