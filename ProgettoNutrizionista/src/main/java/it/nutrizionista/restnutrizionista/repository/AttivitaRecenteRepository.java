package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.AttivitaRecente;

@Repository
public interface AttivitaRecenteRepository extends JpaRepository<AttivitaRecente, Long> {

    /** Per l'upsert: l'eventuale attività già esistente per la coppia (nutrizionista, cliente). */
    Optional<AttivitaRecente> findByNutrizionista_IdAndCliente_Id(Long nutrizionistaId, Long clienteId);

    /** Ultime N attività del nutrizionista, più recenti prima. */
    List<AttivitaRecente> findByNutrizionista_IdOrderByDataAttivitaDesc(Long nutrizionistaId, Pageable pageable);

    /**
     * Cancella le righe di attività recente di un cliente. La FK `cliente_id` NON è in cascade
     * dal Cliente (AttivitaRecente non è una collezione di Cliente): va rimossa esplicitamente nel
     * delete del cliente, altrimenti viola il vincolo FK (DataIntegrityViolation → 400).
     */
    void deleteByCliente_Id(Long clienteId);
}
