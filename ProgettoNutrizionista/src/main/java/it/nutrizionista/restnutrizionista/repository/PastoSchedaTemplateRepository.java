package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.PastoSchedaTemplate;

public interface PastoSchedaTemplateRepository extends JpaRepository<PastoSchedaTemplate, Long> {

	/**
	 * Ownership check: verifica che il pasto appartenga al nutrizionista
	 * PastoSchedaTemplate → SchedaTemplate → createdBy
	 */
	Optional<PastoSchedaTemplate> findByIdAndSchedaTemplate_CreatedBy_Id(Long id, Long userId);

	/**
	 * Lista pasti di un template ordinati per visualizzazione
	 */
	List<PastoSchedaTemplate> findBySchedaTemplate_IdOrderByOrdineVisualizzazioneAsc(Long templateId);
}
