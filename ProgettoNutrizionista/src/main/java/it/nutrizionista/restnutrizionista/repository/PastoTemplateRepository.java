package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.dto.PastoTemplateNameDto;
import it.nutrizionista.restnutrizionista.entity.PastoTemplate;

public interface PastoTemplateRepository extends JpaRepository<PastoTemplate, Long> {

	/**
	 * Proiezione leggera per il dropdown "Aggiungi/applica template": solo id e
	 * nome, senza fetch dell'albero (alimenti/alternative). Niente N+1 né payload
	 * superfluo rispetto a {@link #findByCreatedByIdWithFullTree}.
	 */
	@Query("SELECT new it.nutrizionista.restnutrizionista.dto.PastoTemplateNameDto(t.id, t.nome) " +
	       "FROM PastoTemplate t WHERE t.createdBy.id = :createdById ORDER BY t.nome ASC")
	List<PastoTemplateNameDto> findNamesByCreatedById(@Param("createdById") Long createdById);

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
