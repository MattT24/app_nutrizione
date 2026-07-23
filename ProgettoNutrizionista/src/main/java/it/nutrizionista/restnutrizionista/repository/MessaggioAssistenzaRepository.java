package it.nutrizionista.restnutrizionista.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.MessaggioAssistenza;

public interface MessaggioAssistenzaRepository extends JpaRepository<MessaggioAssistenza, Long> {

    /** F-USER-DEL: TUTTI i messaggi dei ticket del nutrizionista (a prescindere dal mittente: anche le
     *  risposte del super admin), da rimuovere PRIMA dei ticket (FK ticket_id). */
    void deleteByTicket_Nutrizionista_Id(Long nutrizionistaId);
    @Query("SELECT m FROM MessaggioAssistenza m JOIN FETCH m.mittente "
            + "WHERE m.ticket.id = :ticketId AND m.id > :afterId ORDER BY m.id ASC")
    List<MessaggioAssistenza> findIncrementali(@Param("ticketId") Long ticketId,
            @Param("afterId") Long afterId, Pageable pageable);

    long countByTicket_IdAndLettoFalseAndMittente_IdNot(Long ticketId, Long lettoreId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MessaggioAssistenza m SET m.letto = true "
            + "WHERE m.ticket.id = :ticketId AND m.mittente.id <> :lettoreId AND m.letto = false")
    int marcaRicevutiComeLetti(@Param("ticketId") Long ticketId, @Param("lettoreId") Long lettoreId);
}
