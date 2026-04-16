package it.nutrizionista.restnutrizionista.repository;

import it.nutrizionista.restnutrizionista.entity.Promemoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromemoriaRepository extends JpaRepository<Promemoria, Long> {

    @Query("SELECT p FROM Promemoria p WHERE p.nutrizionista.id = :nutrizionistaId " +
           "AND ((p.data <= :endDate AND p.endData >= :startDate) " +
           "OR (p.endData IS NULL AND p.data BETWEEN :startDate AND :endDate)) " +
           "ORDER BY p.data ASC, p.ora ASC")
    List<Promemoria> findByNutrizionistaIdAndDateRange(
            @Param("nutrizionistaId") Long nutrizionistaId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<Promemoria> findByIdAndNutrizionista_Id(Long id, Long nutrizionistaId);
}
