package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;

public interface SchedaTemplateRepository extends JpaRepository<SchedaTemplate, Long> {

	List<SchedaTemplate> findAllByCreatedByIdOrderByUpdatedAtDesc(Long createdById);

	/**
	 * Carica il singolo template con pasti → alimenti → alimento base → macronutrienti.
	 * Il JOIN FETCH su {@code a.macroNutrienti} è essenziale: è un {@code @OneToOne} inverso
	 * (mappedBy) che Hibernate NON batcha con default_batch_fetch_size → senza fetch genera
	 * un N+1 (una query macro per ogni alimento, vedi {@code SchedaRepository.findByIdWithFullDetailsMine}).
	 * {@code pasti}/{@code alimenti} sono {@code Set} (non bag) → fetchabili insieme senza
	 * MultipleBagFetchException. Le alternative restano in query separata
	 * ({@link #fetchAlternativeWithMacroBySchedaId}) per non aggiungere un terzo livello
	 * di prodotto cartesiano (pasti × alimenti × alternative).
	 */
	@Query("""
		SELECT DISTINCT st FROM SchedaTemplate st
		LEFT JOIN FETCH st.pasti p
		LEFT JOIN FETCH p.alimenti ap
		LEFT JOIN FETCH ap.alimento a
		LEFT JOIN FETCH a.macroNutrienti
		WHERE st.id = :id
		""")
	Optional<SchedaTemplate> findByIdWithFullTree(@Param("id") Long id);

	/**
	 * Pre-carica, per un template, le alternative di ogni alimento con il loro alimento base
	 * e i relativi macronutrienti (di nuovo {@code @OneToOne} inverso → senza fetch sarebbe N+1).
	 * Chiamata nella stessa transazione di {@link #findByIdWithFullTree}: i risultati popolano
	 * il persistence context, così il mapper trova {@code ap.alternative} già inizializzate.
	 * Una sola bag ({@code ap.alternative}) → nessun MultipleBagFetchException.
	 */
	@Query("""
		SELECT DISTINCT ap FROM AlimentoPastoSchedaTemplate ap
		LEFT JOIN FETCH ap.alternative alt
		LEFT JOIN FETCH alt.alimentoAlternativo aa
		LEFT JOIN FETCH aa.macroNutrienti
		WHERE ap.pastoSchedaTemplate.schedaTemplate.id = :id
		""")
	List<AlimentoPastoSchedaTemplate> fetchAlternativeWithMacroBySchedaId(@Param("id") Long id);

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
