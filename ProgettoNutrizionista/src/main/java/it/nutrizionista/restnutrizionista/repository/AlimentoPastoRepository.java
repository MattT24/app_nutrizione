package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;

public interface AlimentoPastoRepository extends JpaRepository<AlimentoPasto, Long> {

	boolean existsByPasto_IdAndAlimento_Id(Long id, Long id2);

	void deleteByPasto_IdAndAlimento_Id(Long id, Long id2);
	
	void deleteByPasto_Id(Long pastoId);

	List<AlimentoPasto> findByPasto_Id(Long pastoId);

	Optional<AlimentoPasto> findByPasto_IdAndAlimento_Id(Long id, Long id2);

	Optional<AlimentoPasto> findByIdAndPasto_Scheda_Cliente_Nutrizionista_Id(Long alimentoPastoId, Long nutrizionistaId);

	/**
	 * Elimina in bulk tutti gli alimenti_pasto di una scheda (via subquery sui pasti).
	 */
	@Modifying
	@Query(value = "DELETE FROM alimenti_pasto WHERE pasto_id IN (SELECT id FROM pasti WHERE scheda_id = :schedaId)", nativeQuery = true)
	void bulkDeleteBySchedaId(@Param("schedaId") Long schedaId);

}
