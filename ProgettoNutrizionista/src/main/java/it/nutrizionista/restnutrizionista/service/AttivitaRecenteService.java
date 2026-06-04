package it.nutrizionista.restnutrizionista.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AttivitaRecenteDto;
import it.nutrizionista.restnutrizionista.dto.AttivitaTrackRequest;
import it.nutrizionista.restnutrizionista.entity.AttivitaRecente;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.AttivitaRecenteRepository;

/** Tracciamento e lettura delle attività recenti del nutrizionista sui clienti. */
@Service
public class AttivitaRecenteService {

    @Autowired private AttivitaRecenteRepository repo;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private OwnershipValidator ownershipValidator;

    /**
     * Upsert: aggiorna l'attività esistente per la coppia (nutrizionista, cliente) oppure
     * ne crea una nuova, mantenendo tipo e timestamp più recenti.
     */
    @Transactional
    public void track(AttivitaTrackRequest req) {
        Utente me = currentUserService.getMe();
        Cliente cliente = ownershipValidator.getOwnedCliente(req.getClienteId());
        String tipo = (req.getTipo() == null || req.getTipo().isBlank()) ? "Attività" : req.getTipo();

        AttivitaRecente att = repo
                .findByNutrizionista_IdAndCliente_Id(me.getId(), cliente.getId())
                .orElseGet(() -> {
                    AttivitaRecente nuova = new AttivitaRecente();
                    nuova.setNutrizionista(me);
                    nuova.setCliente(cliente);
                    return nuova;
                });

        att.setTipo(tipo);
        att.setDataAttivita(Instant.now());
        repo.save(att);
    }

    /** Ultime {@code limit} attività del nutrizionista loggato, più recenti prima. */
    @Transactional(readOnly = true)
    public List<AttivitaRecenteDto> getRecenti(int limit) {
        Utente me = currentUserService.getMe();
        return repo.findByNutrizionista_IdOrderByDataAttivitaDesc(me.getId(), PageRequest.of(0, limit))
                .stream()
                .map(a -> new AttivitaRecenteDto(
                        a.getCliente().getId(),
                        a.getCliente().getNome(),
                        a.getCliente().getCognome(),
                        a.getTipo(),
                        a.getDataAttivita()
                ))
                .toList();
    }
}
