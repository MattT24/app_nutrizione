package it.nutrizionista.restnutrizionista.repository;

import it.nutrizionista.restnutrizionista.entity.Appuntamento;

// IMPORT CORRETTO PER JPA (Sostituito quello jdbc)
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppuntamentoRepository extends JpaRepository<Appuntamento, Long> {
    
    // Trova tutti gli appuntamenti generici che si intersecano nel range di date
    List<Appuntamento> findByDataBetweenOrEndDataBetweenOrderByDataAscOraAsc(
        LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2);
    
    // Utile per controlli di sicurezza (es. eliminazione/modifica)
    Optional<Appuntamento> findByIdAndNutrizionista_Id(Long id, Long nutrizionistaId);
    
    // Recupera tutti gli appuntamenti del nutrizionista loggato nel range visibile del calendario
    // (Usato anche dal Service per calcolare le sovrapposizioni)
    @Query("SELECT a FROM Appuntamento a WHERE a.nutrizionista.id = :nutrizionistaId " +
           "AND a.data <= :end AND (a.endData >= :start OR a.endData IS NULL) " +
           "ORDER BY a.data ASC, a.ora ASC")
    List<Appuntamento> findByNutrizionistaIdAndDateRange(
            @Param("nutrizionistaId") Long nutrizionistaId,
            @Param("start") LocalDate start, 
            @Param("end") LocalDate end
    );

    List<Appuntamento> findTop4ByNutrizionistaIdAndDataGreaterThanEqualOrderByDataAscOraAsc(
            @Param("nutrizionistaId") Long nutrizionistaId, @Param("data") LocalDate data);

    /**
     * Elimina tutti gli appuntamenti collegati a un cliente.
     * La FK appuntamenti.cliente_id NON è gestita in cascade dall'entità Cliente,
     * quindi va rimossa esplicitamente prima di cancellare il cliente.
     */
    @Modifying
    @Query("DELETE FROM Appuntamento a WHERE a.cliente.id = :clienteId")
    void deleteByCliente_Id(@Param("clienteId") Long clienteId);

    @Query("SELECT a FROM Appuntamento a WHERE a.data = :data AND a.stato <> :stato AND a.emailCliente IS NOT NULL AND a.emailCliente <> ''")
    List<Appuntamento> findByDataAndStatoNotWithEmail(
            @Param("data") LocalDate data,
            @Param("stato") Appuntamento.StatoAppuntamento stato);

    /**
     * Appuntamenti PRENOTATI del nutrizionista da una certa data in poi, ordinati per
     * data/ora. Consente di calcolare il "prossimo appuntamento" di ogni cliente in
     * un'unica query (scorrendo la lista ordinata si tiene la prima occorrenza per
     * cliente), evitando il problema N+1.
     */
    List<Appuntamento> findByNutrizionistaIdAndStatoAndDataGreaterThanEqualOrderByDataAscOraAsc(
            Long nutrizionistaId, Appuntamento.StatoAppuntamento stato, LocalDate data);
}