package it.nutrizionista.restnutrizionista.dto;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;

/** Riga dello storico accessi ai dati sanitari (audit A7) esposta dall'endpoint di consultazione. */
public record AuditLogDto(
        Long id,
        Long utenteId,
        String utenteEmail,
        AuditAction action,
        AuditEntityType entityType,
        Long entityId,
        Long clienteId,
        AuditOutcome esito,
        String ipAddress,
        String userAgent,
        String destinatario,
        Instant createdAt
) {}
