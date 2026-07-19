package it.nutrizionista.restnutrizionista.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.CredenzialeDemo;
import jakarta.persistence.LockModeType;

public interface CredenzialeDemoRepository extends JpaRepository<CredenzialeDemo, Long> {
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUtente_Id(Long utenteId);

    @EntityGraph(attributePaths = {
        "utente", "utente.ruolo", "utente.ruolo.ruoloPermessi",
        "utente.ruolo.ruoloPermessi.permesso", "utente.ruolo.ruoloPermessi.permesso.gruppo"
    })
    Optional<CredenzialeDemo> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "utente")
    Optional<CredenzialeDemo> findConUtenteById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CredenzialeDemo c JOIN FETCH c.utente WHERE c.id = :id")
    Optional<CredenzialeDemo> findLockedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "utente")
    Page<CredenzialeDemo> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "utente")
    Page<CredenzialeDemo> findByDisabledAtIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "utente")
    Page<CredenzialeDemo> findByDisabledAtIsNullAndExpiresAtLessThanEqualOrderByCreatedAtDesc(
            Instant now, Pageable pageable);

    @EntityGraph(attributePaths = "utente")
    Page<CredenzialeDemo> findByDisabledAtIsNullAndExpiresAtGreaterThanOrderByCreatedAtDesc(
            Instant now, Pageable pageable);
}
