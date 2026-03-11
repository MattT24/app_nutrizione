package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.PastoTemplate;

public interface PastoTemplateRepository extends JpaRepository<PastoTemplate, Long> {
	List<PastoTemplate> findByCreatedBy_IdOrderByUpdatedAtDesc(Long createdById);

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
