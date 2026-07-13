package it.nutrizionista.restnutrizionista.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.Utente;

/** Repository CRUD per Utente + finder per email (login). */
public interface UtenteRepository extends JpaRepository<Utente, Long> {
    Optional<Utente> findByEmail(String email);
    List<Utente> findByRuolo_Alias(String alias);
    Optional<Utente> findByCodiceFiscale(String codiceFiscale);

    /** Conteggi per la dashboard super admin. */
    long countByRuolo_Alias(String alias);
    long countByRuolo_AliasAndLastLoginAtAfter(String alias, Instant after);
    List<Utente> findByRuolo_AliasOrderByCognomeAscNomeAsc(String alias);

    /** Carica utente con ruolo e permessi del ruolo già inizializzati. */
    @Query("SELECT u FROM Utente u " +
            "LEFT JOIN FETCH u.ruolo r " +
            "LEFT JOIN FETCH r.ruoloPermessi rp " +
            "LEFT JOIN FETCH rp.permesso p " +
            "LEFT JOIN FETCH p.gruppo " +
            "WHERE u.email = :email")
     Optional<Utente> findWithAuthoritiesByEmail(@Param("email") String email);
 
}
