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
import it.nutrizionista.restnutrizionista.entity.Utente;
import jakarta.validation.Valid;

public interface AlimentoBaseRepository extends JpaRepository<AlimentoBase, Long>{

	@Override
	@EntityGraph(attributePaths = {"macroNutrienti"})
	Page<AlimentoBase> findAll(Pageable pageable);

	Optional<AlimentoBase> findByNome(@Valid String nome);

	/** Dedup import OFF: un barcode è unico per (created_by, barcode). Vedi piano §6. */
	Optional<AlimentoBase> findByCreatedByAndBarcode(Utente createdBy, String barcode);

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

	/** Alimenti visibili: globali (createdBy IS NULL) + propri.
	 *  NB: NON includere collection (tracce) nell'EntityGraph con Pageable
	 *  altrimenti Hibernate forza paginazione in-memoria (HHH90003004).
	 */
	@EntityGraph(attributePaths = {"macroNutrienti"})
	@Query("SELECT a FROM AlimentoBase a WHERE a.createdBy IS NULL OR a.createdBy.id = :utenteId")
	Page<AlimentoBase> findVisibleByUtente(@Param("utenteId") Long utenteId, Pageable pageable);

	/** Catalogo completo light (globali + propri) per l'indice di ricerca client-side (Fuse.js).
	 *  Niente paginazione: il catalogo è volutamente piccolo. Le tracce sono caricate in batch
	 *  (default_batch_fetch_size) dentro la transazione, non nell'EntityGraph (eviterebbe il
	 *  prodotto cartesiano con macroNutrienti). */
	@EntityGraph(attributePaths = {"macroNutrienti"})
	@Query("SELECT a FROM AlimentoBase a WHERE a.createdBy IS NULL OR a.createdBy.id = :utenteId ORDER BY lower(a.nome) ASC")
	List<AlimentoBase> findVisibleByUtenteList(@Param("utenteId") Long utenteId);

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

	/** Ricerca filtrata per utente — restituisce solo gli ID rankati (prefisso-first),
	 *  con LIMIT applicato via Pageable a livello SQL. Le entità complete (macro+tracce)
	 *  vengono poi caricate in batch con findAllByIdInWithMacro, evitando JOIN FETCH di
	 *  collection + Pageable (HHH90003004). */
	@Query("""
		SELECT a.id
		FROM AlimentoBase a
		WHERE (a.createdBy IS NULL OR a.createdBy.id = :utenteId)
		  AND lower(a.nome) LIKE concat('%', lower(:query), '%')
		ORDER BY
			CASE
				WHEN lower(a.nome) LIKE concat(lower(:query), '%') THEN 0
				ELSE 1
			END,
			lower(a.nome) ASC
	""")
	List<Long> searchByNomeRankedIdsForUser(@Param("query") String query, @Param("utenteId") Long utenteId, Pageable pageable);

	/** Più utilizzati per il nutrizionista corrente — restituisce solo gli ID
	 *  perché @EntityGraph viene ignorato con GROUP BY */
	@Query("""
		SELECT a.id
		FROM AlimentoPasto ap
		JOIN ap.alimento a
		JOIN ap.pasto p
		JOIN p.scheda s
		JOIN s.cliente c
		WHERE c.nutrizionista.id = :utenteId
		GROUP BY a.id
		ORDER BY COUNT(ap.id) DESC
	""")
	List<Long> findTopAlimentiIdsByNutrizionista(@Param("utenteId") Long utenteId, Pageable pageable);

	/**
	 * [Task 18] Classifica globale: alimenti più usati su TUTTA la piattaforma,
	 * indipendentemente dal nutrizionista. Il risultato è pensato per essere
	 * consumato con caching (@Cacheable) dato il costo della COUNT aggregata.
	 */
	@Query("""
		SELECT a.id
		FROM AlimentoPasto ap
		JOIN ap.alimento a
		GROUP BY a.id
		ORDER BY COUNT(ap.id) DESC
	""")
	List<Long> findTopAlimentiIdsGlobal(Pageable pageable);

	/** Carica entità per lista di ID con macro + tracce in una sola query */
	@EntityGraph(attributePaths = {"macroNutrienti", "tracce"})
	@Query("SELECT a FROM AlimentoBase a WHERE a.id IN :ids")
	List<AlimentoBase> findAllByIdInWithMacro(@Param("ids") List<Long> ids);

}

