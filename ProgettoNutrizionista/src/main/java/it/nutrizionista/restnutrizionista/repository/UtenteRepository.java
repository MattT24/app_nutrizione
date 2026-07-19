package it.nutrizionista.restnutrizionista.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.Utente;

import it.nutrizionista.restnutrizionista.enums.MetodoRegistrazione;
/** Repository CRUD per Utente + finder per email (login). */
public interface UtenteRepository extends JpaRepository<Utente, Long> {
    Optional<Utente> findByEmail(String email);
    List<Utente> findByRuolo_Alias(String alias);
    Optional<Utente> findByCodiceFiscale(String codiceFiscale);

    /** Conteggi per la dashboard super admin. */
    long countByRuolo_Alias(String alias);
    long countByRuolo_AliasAndLastLoginAtAfter(String alias, Instant after);
    List<Utente> findByRuolo_AliasOrderByCognomeAscNomeAsc(String alias);
    @Query("""
            select count(u) from Utente u
            where u.ruolo.alias = :alias
              and (u.metodoRegistrazione is null or u.metodoRegistrazione <> :metodo)
            """)
    long countByRuoloExcludingMetodo(@Param("alias") String alias,
            @Param("metodo") MetodoRegistrazione metodo);

    @Query("""
            select count(u) from Utente u
            where u.ruolo.alias = :alias and u.lastLoginAt > :after
              and (u.metodoRegistrazione is null or u.metodoRegistrazione <> :metodo)
            """)
    long countActiveByRuoloExcludingMetodo(@Param("alias") String alias,
            @Param("metodo") MetodoRegistrazione metodo, @Param("after") Instant after);

    @Query("""
            select u from Utente u
            where u.ruolo.alias = :alias
              and (u.metodoRegistrazione is null or u.metodoRegistrazione <> :metodo)
            order by u.cognome asc, u.nome asc
            """)
    List<Utente> findByRuoloExcludingMetodo(@Param("alias") String alias,
            @Param("metodo") MetodoRegistrazione metodo);


    /** Carica utente con ruolo e permessi del ruolo già inizializzati. */
    @Query("SELECT u FROM Utente u " +
            "LEFT JOIN FETCH u.ruolo r " +
            "LEFT JOIN FETCH r.ruoloPermessi rp " +
            "LEFT JOIN FETCH rp.permesso p " +
            "LEFT JOIN FETCH p.gruppo " +
            "WHERE u.email = :email")
     Optional<Utente> findWithAuthoritiesByEmail(@Param("email") String email);
 
}
