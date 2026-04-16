package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.AlimentoSchedaTemplateAlternativa;

public interface AlimentoSchedaTemplateAlternativaRepository extends JpaRepository<AlimentoSchedaTemplateAlternativa, Long> {

	/**
	 * Trova tutte le alternative per un alimento in pasto template, ordinate per priorità
	 */
	List<AlimentoSchedaTemplateAlternativa> findByAlimentoPastoSchedaTemplate_IdOrderByPrioritaAsc(Long aptId);

	/**
	 * Verifica se esiste già questa combinazione alimento_pasto_template + alimento_alternativo
	 */
	boolean existsByAlimentoPastoSchedaTemplate_IdAndAlimentoAlternativo_Id(Long aptId, Long alimentoAlternativoId);

	/**
	 * Conta le alternative per un alimento in pasto template
	 */
	long countByAlimentoPastoSchedaTemplate_Id(Long aptId);

	/**
	 * Ownership check: verifica che l'alternativa appartenga al nutrizionista via catena
	 * AlimentoSchedaTemplateAlternativa → AlimentoPastoSchedaTemplate → PastoSchedaTemplate → SchedaTemplate → createdBy
	 */
	Optional<AlimentoSchedaTemplateAlternativa>
		findByIdAndAlimentoPastoSchedaTemplate_PastoSchedaTemplate_SchedaTemplate_CreatedBy_Id(Long id, Long userId);

	/**
	 * Carica in bulk tutte le alternative per una lista di AlimentoPastoSchedaTemplate IDs.
	 * JOIN FETCH su alimentoAlternativo per evitare lazy loading successivo.
	 * Usato da clonaPastiSuScheda() per eliminare il pattern N+1.
	 */
	@Query("""
		SELECT a FROM AlimentoSchedaTemplateAlternativa a
		JOIN FETCH a.alimentoAlternativo
		WHERE a.alimentoPastoSchedaTemplate.id IN :aptIds
		ORDER BY a.alimentoPastoSchedaTemplate.id, a.priorita
	""")
	List<AlimentoSchedaTemplateAlternativa> findAllByAptIdInWithAlimento(@Param("aptIds") List<Long> aptIds);
}
