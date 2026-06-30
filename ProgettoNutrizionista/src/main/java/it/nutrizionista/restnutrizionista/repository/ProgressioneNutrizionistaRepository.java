package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.ProgressioneNutrizionista;

@Repository
public interface ProgressioneNutrizionistaRepository extends JpaRepository<ProgressioneNutrizionista, Long> {

    Optional<ProgressioneNutrizionista> findByNutrizionista_Id(Long nutrizionistaId);

    /**
     * Incremento atomico dei punti totali e del saldo riscattabile (UPDATE diretto, non
     * read-modify-write): evita di perdere punti quando due eventi per lo stesso nutrizionista
     * vengono registrati in concorrenza. I due delta sono separati perché i punti riscattabili
     * crescono solo per il primo evento di un certo tipo nella giornata (vedi
     * {@link GamificationService}), mentre i punti totali (livelli) crescono sempre. Ritorna il
     * numero di righe aggiornate: 0 se non esiste ancora una riga per questo nutrizionista (il
     * chiamante deve allora crearla).
     */
    @Modifying
    @Query("UPDATE ProgressioneNutrizionista p SET p.puntiTotali = p.puntiTotali + :deltaTotali, "
            + "p.puntiRiscattabili = p.puntiRiscattabili + :deltaRiscattabili WHERE p.nutrizionista.id = :nutrizionistaId")
    int incrementaPunti(@Param("nutrizionistaId") Long nutrizionistaId,
            @Param("deltaTotali") int deltaTotali, @Param("deltaRiscattabili") int deltaRiscattabili);

    /**
     * Scala atomicamente {@code costo} punti dal saldo riscattabile, ma solo se il saldo è
     * sufficiente (condizione nella WHERE): evita che due riscatti concorrenti portino il saldo
     * sotto zero. Ritorna 0 righe aggiornate se il saldo non era (più) sufficiente.
     */
    @Modifying
    @Query("UPDATE ProgressioneNutrizionista p SET p.puntiRiscattabili = p.puntiRiscattabili - :costo "
            + "WHERE p.nutrizionista.id = :nutrizionistaId AND p.puntiRiscattabili >= :costo")
    int scalaPuntiRiscattabili(@Param("nutrizionistaId") Long nutrizionistaId, @Param("costo") int costo);
}
