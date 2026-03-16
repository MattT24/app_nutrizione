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
	@EntityGraph(attributePaths = {"macroNutrienti", "tracce"})
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

	@Query("SELECT DISTINCT a.categoria FROM AlimentoBase a WHERE a.categoria IS NOT NULL ORDER BY a.categoria")
	List<String> findDistinctCategorie();

	/** Alimenti visibili: globali (createdBy IS NULL) + propri */
	@EntityGraph(attributePaths = {"macroNutrienti"})
	@Query("SELECT a FROM AlimentoBase a WHERE a.createdBy IS NULL OR a.createdBy.id = :utenteId")
	Page<AlimentoBase> findVisibleByUtente(@Param("utenteId") Long utenteId, Pageable pageable);

	/** Categorie visibili per un utente */
	@Query("SELECT DISTINCT a.categoria FROM AlimentoBase a WHERE a.categoria IS NOT NULL AND (a.createdBy IS NULL OR a.createdBy.id = :utenteId) ORDER BY a.categoria")
	List<String> findDistinctCategorieForUser(@Param("utenteId") Long utenteId);

	/** Ricerca filtrata per utente — LOWER()+LIKE per case-insensitive, JOIN FETCH tracce per filtri esclusione */
	@Query("""
		SELECT a
		FROM AlimentoBase a
		LEFT JOIN FETCH a.macroNutrienti
		LEFT JOIN FETCH a.tracce
		WHERE (a.createdBy IS NULL OR a.createdBy.id = :utenteId)
		  AND lower(a.nome) LIKE concat('%', lower(:query), '%')
		ORDER BY
			CASE
				WHEN lower(a.nome) LIKE concat(lower(:query), '%') THEN 0
				ELSE 1
			END,
			lower(a.nome) ASC
	""")
	List<AlimentoBase> searchByNomeRankedForUser(@Param("query") String query, @Param("utenteId") Long utenteId);

	/** Più utilizzati per il nutrizionista corrente */
	@Query("""
		SELECT a
		FROM AlimentoPasto ap
		JOIN ap.alimento a
		JOIN ap.pasto p
		JOIN p.scheda s
		JOIN s.cliente c
		WHERE c.nutrizionista.id = :utenteId
		GROUP BY a
		ORDER BY COUNT(ap.id) DESC
	""")
	List<AlimentoBase> findTopAlimentiByNutrizionista(@Param("utenteId") Long utenteId, Pageable pageable);

}
