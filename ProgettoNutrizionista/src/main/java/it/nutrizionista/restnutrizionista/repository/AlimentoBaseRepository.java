package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import jakarta.validation.Valid;

public interface AlimentoBaseRepository extends JpaRepository<AlimentoBase, Long>{

	@Override
	@EntityGraph(attributePaths = {"macroNutrienti"})
	Page<AlimentoBase> findAll(Pageable pageable);

	Optional<AlimentoBase> findByNome(@Valid String nome);

	List<AlimentoBase> findByNomeContainingIgnoreCase(String query);

	@Query("""
		SELECT a
		FROM AlimentoBase a
		LEFT JOIN FETCH a.macroNutrienti
		WHERE lower(a.nome) LIKE concat('%', lower(:query), '%')
		ORDER BY
			CASE
				WHEN lower(a.nome) LIKE concat(lower(:query), '%') THEN 0
				ELSE 1
			END,
			lower(a.nome) ASC
	""")
	List<AlimentoBase> searchByNomeRanked(@Param("query") String query);

	/**
	 * Carica un AlimentoBase con tutti i dettagli (macro, micronutrienti, tracce)
	 * in una singola query, evitando N+1 e LazyInitializationException su tracce.
	 */
	@Query("SELECT DISTINCT a FROM AlimentoBase a " +
	       "LEFT JOIN FETCH a.macroNutrienti " +
	       "LEFT JOIN FETCH a.micronutrienti vm " +
	       "LEFT JOIN FETCH vm.micronutriente " +
	       "LEFT JOIN FETCH a.tracce " +
	       "WHERE a.id = :id")
	Optional<AlimentoBase> findByIdWithDetails(@Param("id") Long id);

}
