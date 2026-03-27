package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.Ricetta;

@Repository
public interface RicettaRepository extends JpaRepository<Ricetta, Long> {

    /** Tutte le ricette pubbliche, ordinate per titolo. */
    List<Ricetta> findByPubblicaTrueOrderByTitoloAsc();

    /** Filtra per categoria. */
    List<Ricetta> findByPubblicaTrueAndCategoriaOrderByTitoloAsc(String categoria);

    /** Ricerca per titolo (case-insensitive LIKE). */
    @Query("SELECT r FROM Ricetta r WHERE r.pubblica = true AND LOWER(r.titolo) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY r.titolo ASC")
    List<Ricetta> searchByTitolo(@Param("query") String query);

    /** Fetch con ingredienti caricati eagerly per evitare N+1 in dettaglio. */
    @Query("SELECT r FROM Ricetta r LEFT JOIN FETCH r.ingredienti ing LEFT JOIN FETCH ing.alimento WHERE r.id = :id")
    Optional<Ricetta> findByIdWithIngredienti(@Param("id") Long id);

    /** Lista con ingredienti pre-caricati (usata per la lista pubblica completa). */
    @Query("SELECT DISTINCT r FROM Ricetta r LEFT JOIN FETCH r.ingredienti ing LEFT JOIN FETCH ing.alimento WHERE r.pubblica = true ORDER BY r.titolo ASC")
    List<Ricetta> findAllPublicWithIngredienti();
}
