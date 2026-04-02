package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;

public interface AlimentoAlternativoRepository extends JpaRepository<AlimentoAlternativo, Long> {

    /**
     * Trova tutte le alternative per un alimento in un pasto, ordinate per priorità
     */
    List<AlimentoAlternativo> findByAlimentoPasto_IdOrderByPrioritaAsc(Long alimentoPastoId);

    /**
     * Verifica se esiste già questa combinazione alimento_pasto + alimento_alternativo
     */
    boolean existsByAlimentoPasto_IdAndAlimentoAlternativo_Id(Long alimentoPastoId, Long alimentoAlternativoId);

    /**
     * Trova una specifica alternativa per alimento_pasto e alimento_alternativo
     */
    Optional<AlimentoAlternativo> findByAlimentoPasto_IdAndAlimentoAlternativo_Id(Long alimentoPastoId, Long alimentoAlternativoId);

    Optional<AlimentoAlternativo> findByIdAndAlimentoPasto_Pasto_Scheda_Cliente_Nutrizionista_Id(Long id, Long nutrizionistaId);

    /**
     * Conta le alternative per un alimento in pasto
     */
    long countByAlimentoPasto_Id(Long alimentoPastoId);

    /**
     * Elimina tutte le alternative di un alimento in pasto
     */
    void deleteByAlimentoPasto_Id(Long alimentoPastoId);

    /**
     * Elimina tutte le alternative di un pasto (usato da copyDay per pulizia)
     */
    void deleteByPasto_Id(Long pastoId);

    boolean existsByPasto_IdAndAlimentoAlternativo_Id(Long pastoId, Long alimentoAlternativoId);

    /**
     * Carica in bulk tutte le alternative per una lista di alimento_pasto IDs.
     * JOIN FETCH su alimentoAlternativo per evitare lazy loading successivo.
     * Usato da deepCloneAlternatives() per eliminare il pattern N+1.
     */
    @Query("""
        SELECT aa FROM AlimentoAlternativo aa
        JOIN FETCH aa.alimentoAlternativo
        WHERE aa.alimentoPasto.id IN :apIds
        ORDER BY aa.alimentoPasto.id, aa.priorita
    """)
    List<AlimentoAlternativo> findAllByAlimentoPastoIds(@Param("apIds") List<Long> apIds);

    /**
     * Elimina in bulk tutte le alternative di una scheda (via subquery sui pasti).
     * Usato dal delete ottimizzato per evitare N DELETE individuali.
     */
    @Modifying
    @Query(value = "DELETE FROM alimenti_alternativi WHERE pasto_id IN (SELECT id FROM pasti WHERE scheda_id = :schedaId) OR alimento_pasto_id IN (SELECT ap.id FROM alimenti_pasto ap JOIN pasti p ON ap.pasto_id = p.id WHERE p.scheda_id = :schedaId)", nativeQuery = true)
    void bulkDeleteBySchedaId(@Param("schedaId") Long schedaId);

}
