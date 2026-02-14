package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.nutrizionista.restnutrizionista.entity.Pasto;

public interface PastoRepository extends JpaRepository<Pasto, Long> {

	@Query("SELECT p FROM Pasto p WHERE p.scheda.cliente.nutrizionista.id = :nutrizionistaId")
    Page<Pasto> findByNutrizionista_Id(Long nutrizionistaId, Pageable pageable);

	Optional<Pasto> findByIdAndScheda_Cliente_Nutrizionista_Id(Long id, Long nutrizionistaId);
}
