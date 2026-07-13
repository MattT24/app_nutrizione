package it.nutrizionista.restnutrizionista.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.enums.AuditAction;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Consultazione paginata dello storico accessi con filtri opzionali (guardie {@code :x IS NULL}).
     * Sfrutta gli indici {@code (cliente_id, created_at)} / {@code (utente_id, created_at)} / {@code (action, created_at)}.
     */
    @Query("SELECT a FROM AuditLog a WHERE "
            + "(:clienteId IS NULL OR a.clienteId = :clienteId) AND "
            + "(:utenteId IS NULL OR a.utenteId = :utenteId) AND "
            + "(:action IS NULL OR a.action = :action) AND "
            + "(:from IS NULL OR a.createdAt >= :from) AND "
            + "(:to IS NULL OR a.createdAt <= :to) "
            + "ORDER BY a.createdAt DESC")
    Page<AuditLog> search(@Param("clienteId") Long clienteId,
                          @Param("utenteId") Long utenteId,
                          @Param("action") AuditAction action,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);

    /** Retention: elimina le righe più vecchie della soglia (usato dal cleanup notturno). */
    @Modifying
    int deleteByCreatedAtBefore(Instant soglia);
}
