package it.nutrizionista.restnutrizionista.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.time.LocalDate;

import it.nutrizionista.restnutrizionista.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{
    
    // Cerca per ID e Nutrizionista (per sicurezza rapida)
    Optional<Cliente> findByIdAndNutrizionista_Id(Long id, Long nutrizionistaId);

    // Cerca paginata per nutrizionista
    Page<Cliente> findByNutrizionista_Id(Long nutrizionistaId, Pageable pageable);

    Page<Cliente> findByNutrizionista_IdOrderByCreatedAtDesc(Long nutrizionistaId, Pageable pageable);

    @Query("""
            select c from Cliente c
            where c.nutrizionista.id = :nutrizionistaId
              and (
                lower(c.nome) like lower(concat('%', :q, '%'))
                or lower(c.cognome) like lower(concat('%', :q, '%'))
                or lower(concat(c.nome, ' ', c.cognome)) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.telefono, '')) like lower(concat('%', :q, '%'))
              )
            order by c.createdAt desc
            """)
    Page<Cliente> searchForDemoAdmin(
            @Param("nutrizionistaId") Long nutrizionistaId,
            @Param("q") String q,
            Pageable pageable);
    
    // Ricerca parziale (es. "Mar" trova "Mario") limitata al nutrizionista
    List<Cliente> findByNutrizionista_IdAndNomeContainingIgnoreCase(Long nutrizionistaId, String nome);
    
    List<Cliente> findByNutrizionista_IdAndCognomeContainingIgnoreCase(Long nutrizionistaId, String cognome);
    
 // Cerca tutti i clienti per nutrizionista (Lista completa senza paginazione)
    List<Cliente> findByNutrizionista_Id(Long nutrizionistaId);

    // Conteggio totale clienti del nutrizionista (gamification: badge di crescita studio)
    long countByNutrizionista_Id(Long nutrizionistaId);

    interface ConteggioClientiPerNutrizionista {
        Long getNutrizionistaId();
        long getTotale();
    }

    @Query("""
            select c.nutrizionista.id as nutrizionistaId, count(c.id) as totale
            from Cliente c
            where c.nutrizionista.id in :nutrizionistaIds
            group by c.nutrizionista.id
            """)
    List<ConteggioClientiPerNutrizionista> countByNutrizionistaIds(
            @Param("nutrizionistaIds") Collection<Long> nutrizionistaIds);
    
    // Verifica duplicati codice fiscale
    boolean existsByCodiceFiscale(String codiceFiscale);

    // Verifica duplicati email
    boolean existsByEmail(String email);

    // Varianti per l'update: escludono il cliente stesso
    boolean existsByCodiceFiscaleAndIdNot(String codiceFiscale, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
    
    @Query("""
    	    select c from Cliente c
    	    where c.nutrizionista.id = :nutrizionistaId
    	      and (
    	        lower(c.nome) like lower(concat('%', :q, '%'))
    	        or lower(c.cognome) like lower(concat('%', :q, '%'))
    	        or lower(concat(c.nome, ' ', c.cognome)) like lower(concat('%', :q, '%'))
    	        or lower(concat(c.cognome, ' ', c.nome)) like lower(concat('%', :q, '%'))
    	      )
    	    order by c.cognome asc, c.nome asc
    	""")
    	List<Cliente> searchMyClientsByName(
    	    @Param("nutrizionistaId") Long nutrizionistaId,
    	    @Param("q") String q
    	);
    //Questo garantisce che il nutrizionista vede solo i suoi clienti
    @Query("select c from Cliente c where c.id = :id and c.nutrizionista.id = :nutrizionistaId")
    Cliente findMineById(@Param("id") Long id, @Param("nutrizionistaId") Long nutrizionistaId);

    // ═══════════ A6 Retention (art. 5(1)(e)) — eleggibilità alla cancellazione ═══════════
    /**
     * Predicato CONDIVISO tra la query batch ({@link #findInattiviPerRetention}) e il re-check per-id in-tx
     * ({@link #findInattivoById}, TOCTOU): riusato per costruzione → non possono divergere. Un cliente è
     * "inattivo" (eleggibile) se: creato prima del {@code cutoff}, senza contatto clinico d'AGGREGATO recente
     * ({@code ultimoContattoClinico}), e con NESSUN figlio del sotto-albero clinico che abbia attività recente
     * (createdAt/updatedAt >= cutoff). Direzione d'errore = over-retention (mai cancellare un cliente attivo).
     */
    String A6_INATTIVI_PREDICATE = """
            c.createdAt < :cutoff
            AND (c.ultimoContattoClinico IS NULL OR c.ultimoContattoClinico < :cutoff)
            AND NOT EXISTS (SELECT s.id  FROM Scheda s                    WHERE s.cliente = c                             AND (s.createdAt  >= :cutoff OR s.updatedAt  >= :cutoff))
            AND NOT EXISTS (SELECT p.id  FROM Pasto p                     WHERE p.scheda.cliente = c                      AND (p.createdAt  >= :cutoff OR p.updatedAt  >= :cutoff))
            AND NOT EXISTS (SELECT ap.id FROM AlimentoPasto ap            WHERE ap.pasto.scheda.cliente = c               AND (ap.createdAt >= :cutoff OR ap.updatedAt >= :cutoff))
            AND NOT EXISTS (SELECT aa.id FROM AlimentoAlternativo aa      WHERE (aa.alimentoPasto.pasto.scheda.cliente = c OR aa.pasto.scheda.cliente = c) AND (aa.createdAt >= :cutoff OR aa.updatedAt >= :cutoff))
            AND NOT EXISTS (SELECT n.id  FROM AlimentoPastoNomeOverride n WHERE n.alimentoPasto.pasto.scheda.cliente = c  AND (n.createdAt  >= :cutoff OR n.updatedAt  >= :cutoff))
            AND NOT EXISTS (SELECT m.id  FROM MisurazioneAntropometrica m WHERE m.cliente = c                             AND (m.createdAt  >= :cutoff OR m.updatedAt  >= :cutoff))
            AND NOT EXISTS (SELECT pl.id FROM Plicometria pl              WHERE pl.cliente = c                            AND (pl.createdAt >= :cutoff OR pl.updatedAt >= :cutoff))
            AND NOT EXISTS (SELECT o.id  FROM ObiettivoNutrizionale o     WHERE o.cliente = c                             AND (o.createdAt  >= :cutoff OR o.updatedAt  >= :cutoff))
            AND NOT EXISTS (SELECT av.id FROM AvversionePersonale av      WHERE av.cliente = c                            AND (av.createdAt >= :cutoff OR av.updatedAt >= :cutoff))
            AND NOT EXISTS (SELECT d.id  FROM DocumentoFascicolo d        WHERE d.cliente = c                             AND (d.dataCreazione >= :cutoff OR d.dataUltimoInvio >= :cutoff))
            AND NOT EXISTS (SELECT a.id  FROM Appuntamento a              WHERE a.cliente = c                             AND (a.createdAt  >= :cutoff OR a.updatedAt  >= :cutoff))
            AND NOT EXISTS (SELECT t.id  FROM CalcoloTdee t               WHERE t.cliente = c                             AND t.dataCalcolo >= :cutoffDate)
            """;

    /** Clienti eleggibili al ciclo di retention (query notturna). */
    @Query("SELECT c FROM Cliente c WHERE " + A6_INATTIVI_PREDICATE)
    List<Cliente> findInattiviPerRetention(@Param("cutoff") Instant cutoff, @Param("cutoffDate") LocalDate cutoffDate);

    /** Re-check per-id con lo STESSO predicato (TOCTOU: verifica in-tx prima del purge). */
    @Query("SELECT c FROM Cliente c WHERE c.id = :id AND (" + A6_INATTIVI_PREDICATE + ")")
    Optional<Cliente> findInattivoById(@Param("id") Long id, @Param("cutoff") Instant cutoff, @Param("cutoffDate") LocalDate cutoffDate);

    /** Lock riga PRIMA del purge (query SEMPLICE, sicuramente lockabile su H2/TiDB; il lock NON va preso sulla
     *  query con i 12 NOT EXISTS). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cliente c WHERE c.id = :id")
    Optional<Cliente> findByIdConLock(@Param("id") Long id);

    /** Sorgente per il clear della quarantena stale (NON gli inattivi del run corrente). */
    List<Cliente> findByDataQuarantenaIsNotNull();

}