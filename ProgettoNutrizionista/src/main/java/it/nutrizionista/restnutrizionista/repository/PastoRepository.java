package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.Pasto;

public interface PastoRepository extends JpaRepository<Pasto, Long> {

	@Query("SELECT p FROM Pasto p WHERE p.scheda.cliente.nutrizionista.id = :nutrizionistaId")
    Page<Pasto> findByNutrizionista_Id(Long nutrizionistaId, Pageable pageable);

	Optional<Pasto> findByIdAndScheda_Cliente_Nutrizionista_Id(Long id, Long nutrizionistaId);
	
	boolean existsByScheda_IdAndDefaultCodeIgnoreCase(Long schedaId, String defaultCode);
	
	Optional<Pasto> findTopByScheda_IdOrderByOrdineVisualizzazioneDescIdDesc(Long schedaId);
	
	List<Pasto> findByScheda_IdAndDefaultCodeIsNotNull(Long schedaId);
	
	List<Pasto> findByScheda_IdOrderByOrdineVisualizzazioneAscIdAsc(Long schedaId);

	/**
	 * Carica l'intero albero del Pasto in una singola query:
	 * Pasto → alimentiPasto → alimento → macroNutrienti
	 *                        → nomeOverride
	 *                        → alternative → alimentoAlternativo → macroNutrienti
	 *       → scheda → cliente
	 */
	@Query("SELECT DISTINCT p FROM Pasto p " +
	       "LEFT JOIN FETCH p.alimentiPasto ap " +
	       "LEFT JOIN FETCH ap.alimento a " +
	       "LEFT JOIN FETCH a.macroNutrienti " +
	       "LEFT JOIN FETCH ap.nomeOverride " +
	       "LEFT JOIN FETCH ap.alternative alt " +
	       "LEFT JOIN FETCH alt.alimentoAlternativo altAlim " +
	       "LEFT JOIN FETCH altAlim.macroNutrienti " +
	       "WHERE p.id = :id")
	Optional<Pasto> findByIdWithFullTree(@Param("id") Long id);

	/**
	 * Restituisce solo il clienteId associato al pasto (senza caricare Scheda/Cliente).
	 */
	@Query("SELECT s.cliente.id FROM Pasto p JOIN p.scheda s WHERE p.id = :pastoId")
	Optional<Long> findClienteIdByPastoId(@Param("pastoId") Long pastoId);
}
