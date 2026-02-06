package it.nutrizionista.restnutrizionista.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Appuntamento;

public interface AppuntamentoRepository extends JpaRepository<Appuntamento, Long> {

    Optional<Appuntamento> findByIdAndNutrizionista_Id(Long id, Long nutrizionistaId);

    List<Appuntamento> findByNutrizionista_IdAndDataBetween(Long nutrizionistaId, LocalDate start, LocalDate end);

    boolean existsByNutrizionista_IdAndDataAndOra(Long nutrizionistaId, LocalDate data, LocalTime ora);

    // serve per update: “esiste un altro appuntamento alla stessa data/ora?”
    boolean existsByNutrizionista_IdAndDataAndOraAndIdNot(Long nutrizionistaId, LocalDate data, LocalTime ora, Long idNot);
}
