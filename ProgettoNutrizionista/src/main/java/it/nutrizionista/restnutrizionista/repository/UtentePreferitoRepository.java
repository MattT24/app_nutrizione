package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.entity.UtentePreferito;

@Repository
public interface UtentePreferitoRepository extends JpaRepository<UtentePreferito, Long> {

    /**
     * Carica i preferiti con alimento.macroNutrienti e alimento.tracce eager
     * per evitare LazyInitializationException in DtoMapper.toAlimentoBaseDtoLight.
     */
    @Query("""
        SELECT up
        FROM UtentePreferito up
        LEFT JOIN FETCH up.alimento a
        LEFT JOIN FETCH a.macroNutrienti
        LEFT JOIN FETCH a.tracce
        WHERE up.utente.id = :utenteId
        ORDER BY up.createdAt DESC
    """)
    List<UtentePreferito> findByUtenteIdOrderByCreatedAtDesc(@Param("utenteId") Long utenteId);

    Optional<UtentePreferito> findByUtenteIdAndAlimentoId(Long utenteId, Long alimentoId);

    void deleteByUtenteIdAndAlimentoId(Long utenteId, Long alimentoId);

    boolean existsByUtenteIdAndAlimentoId(Long utenteId, Long alimentoId);
}
