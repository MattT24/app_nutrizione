package it.nutrizionista.restnutrizionista.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.EventoGamification;
import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;

@Repository
public interface EventoGamificationRepository extends JpaRepository<EventoGamification, Long> {

    /** Per l'idempotenza dell'accesso giornaliero: c'è già un evento di questo tipo registrato da {@code da} in poi? */
    boolean existsByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
            Long nutrizionistaId, TipoEventoGamification tipoEvento, Instant da);

    /** Ultimi eventi del nutrizionista, più recenti prima (storico per la pagina Traguardi). */
    List<EventoGamification> findByNutrizionista_IdOrderByCreatedAtDesc(Long nutrizionistaId, Pageable pageable);

    /** Eventi di accesso giornaliero dal momento indicato in poi, per calcolare lo streak corrente. */
    List<EventoGamification> findByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
            Long nutrizionistaId, TipoEventoGamification tipoEvento, Instant da);

    /**
     * Elimina gli eventi precedenti a {@code prima}: usato dalla pulizia notturna per non far
     * crescere la tabella all'infinito. Va usato solo con una soglia più vecchia della finestra
     * guardata dallo streak ({@link it.nutrizionista.restnutrizionista.service.GamificationService#STREAK_LOOKBACK_GIORNI}),
     * altrimenti si romperebbe il calcolo dei giorni di fila.
     */
    @Modifying
    int deleteByCreatedAtBefore(Instant prima);

    /**
     * F-DEL-CASCADE — anonimizza (NON cancella) gli eventi gamification riferiti a un cliente
     * cancellato, azzerandone il {@code clienteId}. {@code clienteId} è un {@code Long} denormalizzato
     * SENZA FK (l'unico parent è il nutrizionista): alla cancellazione del cliente resterebbe un
     * riferimento pendente al paziente. Nullarlo recide il legame col dato cancellato (art. 17)
     * preservando lo storico/punti del nutrizionista (dato del professionista, non del paziente).
     * <p>Bulk update JPQL: deterministico e identico su H2 e TiDB. Va invocato nella STESSA
     * transazione del delete del cliente. Contrapposto ad {@code AuditLog.clienteId}, che invece
     * NON si nulla mai (retention A7, obbligo legale art. 17(3)(b)).
     */
    @Modifying
    @Query("UPDATE EventoGamification e SET e.clienteId = NULL WHERE e.clienteId = :clienteId")
    int anonimizzaClienteId(@Param("clienteId") Long clienteId);
}
