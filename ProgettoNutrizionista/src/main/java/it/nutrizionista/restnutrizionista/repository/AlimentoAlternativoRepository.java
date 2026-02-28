package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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

    // === PER-PASTO METHODS ===

    /**
     * Trova tutte le alternative per un pasto con JOIN FETCH su alimentoAlternativo e macro
     */
    @Query("SELECT aa FROM AlimentoAlternativo aa " +
           "LEFT JOIN FETCH aa.alimentoAlternativo a " +
           "LEFT JOIN FETCH a.macroNutrienti " +
           "WHERE aa.pasto.id = :pastoId " +
           "ORDER BY aa.priorita ASC")
    List<AlimentoAlternativo> findByPasto_IdOrderByPrioritaAsc(@Param("pastoId") Long pastoId);

    /**
     * Verifica se esiste già questa combinazione pasto + alimento_alternativo
     */
    boolean existsByPasto_IdAndAlimentoAlternativo_Id(Long pastoId, Long alimentoAlternativoId);

    /**
     * Conta le alternative per un pasto
     */
    long countByPasto_Id(Long pastoId);

    /**
     * Elimina tutte le alternative di un pasto
     */
    void deleteByPasto_Id(Long pastoId);

    // === BATCH PER-SCHEDA ===

    /**
     * Carica tutte le alternative di tutti i pasti di una scheda in una sola query
     */
    @Query("SELECT aa FROM AlimentoAlternativo aa " +
           "LEFT JOIN FETCH aa.alimentoAlternativo a " +
           "LEFT JOIN FETCH a.macroNutrienti " +
           "WHERE aa.pasto.scheda.id = :schedaId " +
           "ORDER BY aa.pasto.id, aa.priorita ASC")
    List<AlimentoAlternativo> findByPasto_Scheda_IdOrderByPastoIdAndPrioritaAsc(@Param("schedaId") Long schedaId);
}
