package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Scheda;

public interface SchedaRepository extends JpaRepository<Scheda, Long> {

	List<Scheda> findByClienteIdAndAttivaTrue(Long id);

	@EntityGraph(attributePaths = {"pasti", "pasti.alimentiPasto"})
	Optional<Scheda> findByIdWithPastiAndAlimenti(Long schedaId);

}
