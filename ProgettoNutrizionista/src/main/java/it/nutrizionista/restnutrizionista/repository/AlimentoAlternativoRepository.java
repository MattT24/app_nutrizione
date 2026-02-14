package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
