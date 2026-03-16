package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.PastoTemplate;

public interface PastoTemplateRepository extends JpaRepository<PastoTemplate, Long> {

	/**
	 * Carica tutti i template del nutrizionista con i relativi alimenti,
	 * alternative e macroNutrienti via JOIN FETCH per evitare LazyInitializationException
	 * nel DtoMapper che opera fuori dalla sessione Hibernate.
	 */
	@Query("SELECT DISTINCT t FROM PastoTemplate t " +
	       "LEFT JOIN FETCH t.alimenti ta " +
	       "LEFT JOIN FETCH ta.alimento a " +
	       "LEFT JOIN FETCH a.macroNutrienti " +
	       "LEFT JOIN FETCH ta.alternative alt " +
	       "LEFT JOIN FETCH alt.alimentoAlternativo altAlim " +
	       "LEFT JOIN FETCH altAlim.macroNutrienti " +
	       "WHERE t.createdBy.id = :createdById " +
	       "ORDER BY t.updatedAt DESC")
	List<PastoTemplate> findByCreatedByIdWithFullTree(@Param("createdById") Long createdById);

	Optional<PastoTemplate> findByIdAndCreatedBy_Id(Long id, Long createdById);

	@Query("SELECT DISTINCT t FROM PastoTemplate t " +
	       "LEFT JOIN FETCH t.alimenti ta " +
	       "LEFT JOIN FETCH ta.alimento a " +
	       "LEFT JOIN FETCH a.macroNutrienti " +
	       "LEFT JOIN FETCH ta.alternative alt " +
	       "LEFT JOIN FETCH alt.alimentoAlternativo altAlim " +
	       "LEFT JOIN FETCH altAlim.macroNutrienti " +
	       "WHERE t.id = :id")
	Optional<PastoTemplate> findByIdWithFullTree(@Param("id") Long id);
}
