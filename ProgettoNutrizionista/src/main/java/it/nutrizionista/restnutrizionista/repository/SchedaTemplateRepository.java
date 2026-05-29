package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;

public interface SchedaTemplateRepository extends JpaRepository<SchedaTemplate, Long> {

	List<SchedaTemplate> findAllByCreatedByIdOrderByUpdatedAtDesc(Long createdById);

	/**
	 * Carica il singolo template con i suoi pasti.
	 * Le collezioni figlie (alimenti, alimento, macroNutrienti) vengono caricate
	 * in batch automaticamente da Hibernate (default_batch_fetch_size=50).
	 * Evitare JOIN FETCH multipli su List → MultipleBagFetchException.
	 */
	@Query("""
		SELECT DISTINCT st FROM SchedaTemplate st
		LEFT JOIN FETCH st.pasti p
		WHERE st.id = :id
		""")
	Optional<SchedaTemplate> findByIdWithFullTree(@Param("id") Long id);

	/**
	 * Lista tutti i template di un nutrizionista con i pasti pre-caricati.
	 * Le collezioni figlie vengono caricate in batch da Hibernate.
	 */
	@Query("""
		SELECT DISTINCT st FROM SchedaTemplate st
		LEFT JOIN FETCH st.pasti p
		WHERE st.createdBy.id = :createdById
		ORDER BY st.id DESC
		""")
	List<SchedaTemplate> findByCreatedByIdWithFullTree(@Param("createdById") Long createdById);
}
