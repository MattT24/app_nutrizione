package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.nutrizionista.restnutrizionista.entity.Scheda;

public interface SchedaRepository extends JpaRepository<Scheda, Long> {

	List<Scheda> findByCliente_IdAndAttivaTrue(Long id);
//TODO da ricontrollare
	@EntityGraph(attributePaths = {"pasti", "pasti.alimentiPasto"})
    @Query("SELECT s FROM Scheda s WHERE s.id = :id")
    Optional<Scheda> findByIdWithPastiAndAlimenti(Long id);
	
	@EntityGraph(attributePaths = {"pasti", "pasti.alimentiPasto"})
    @Query("SELECT s FROM Scheda s WHERE s.id = :id AND s.cliente.nutrizionista.id = :nutrizionistaId")
    Optional<Scheda> findByIdWithPastiAndAlimentiMine(Long id, Long nutrizionistaId);
	
	Optional<Scheda> findByIdAndCliente_Nutrizionista_Id(Long id, Long nutrizionistaId);
	List<Scheda> findByCliente_Id(Long id);
	Page<Scheda> findByCliente_IdOrderByDataCreazioneDescIdDesc(Long clienteId, Pageable pageable);	
}
