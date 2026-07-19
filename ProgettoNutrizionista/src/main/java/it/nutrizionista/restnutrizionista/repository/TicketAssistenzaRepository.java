package it.nutrizionista.restnutrizionista.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.TicketAssistenza;
import it.nutrizionista.restnutrizionista.enums.StatoTicket;

public interface TicketAssistenzaRepository extends JpaRepository<TicketAssistenza, Long> {
    boolean existsByNutrizionista_IdAndStatoIn(Long nutrizionistaId, Collection<StatoTicket> stati);
    Optional<TicketAssistenza> findFirstByNutrizionista_IdAndStatoInOrderByCreatedAtDesc(
            Long nutrizionistaId, Collection<StatoTicket> stati);
    Page<TicketAssistenza> findByNutrizionista_IdOrderByCreatedAtDesc(Long nutrizionistaId, Pageable pageable);
    Page<TicketAssistenza> findByStatoOrderByCreatedAtAsc(StatoTicket stato, Pageable pageable);
    long countByStato(StatoTicket stato);

    @Query("SELECT t FROM TicketAssistenza t JOIN FETCH t.nutrizionista WHERE t.id = :id")
    Optional<TicketAssistenza> findByIdConNutrizionista(@Param("id") Long id);
}
