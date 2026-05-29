package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;

public interface AlimentoPastoSchedaTemplateRepository extends JpaRepository<AlimentoPastoSchedaTemplate, Long> {

	/**
	 * Ownership check: verifica che l'alimento in pasto template appartenga al nutrizionista
	 * AlimentoPastoSchedaTemplate → PastoSchedaTemplate → SchedaTemplate → createdBy
	 */
	Optional<AlimentoPastoSchedaTemplate> findByIdAndPastoSchedaTemplate_SchedaTemplate_CreatedBy_Id(Long id, Long userId);

	/**
	 * Lista alimenti di un pasto template ordinati per ordine
	 */
	List<AlimentoPastoSchedaTemplate> findByPastoSchedaTemplate_IdOrderByOrdineAsc(Long pastoId);
}
