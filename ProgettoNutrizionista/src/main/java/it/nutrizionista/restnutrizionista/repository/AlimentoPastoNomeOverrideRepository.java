package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;

public interface AlimentoPastoNomeOverrideRepository extends JpaRepository<AlimentoPastoNomeOverride, Long> {
	Optional<AlimentoPastoNomeOverride> findByAlimentoPasto_Id(Long alimentoPastoId);
	void deleteByAlimentoPasto_Id(Long alimentoPastoId);
	boolean existsByAlimentoPasto_Id(Long alimentoPastoId);

	/**
	 * Elimina in bulk tutti i nome_override di una scheda (via subquery).
	 */
	@Modifying
	@Query(value = "DELETE FROM alimenti_pasto_nome_override WHERE alimento_pasto_id IN (SELECT ap.id FROM alimenti_pasto ap JOIN pasti p ON ap.pasto_id = p.id WHERE p.scheda_id = :schedaId)", nativeQuery = true)
	void bulkDeleteBySchedaId(@Param("schedaId") Long schedaId);
}
