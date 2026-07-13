package it.nutrizionista.restnutrizionista.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AuditLogDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.service.AuditService;

/**
 * Consultazione dello "storico accessi" ai dati sanitari (A7).
 * <ul>
 *   <li>{@code GET /api/audit} — vista globale, permesso {@code AUDIT_READ} (titolare/admin).</li>
 *   <li>{@code GET /api/audit/me} — self-audit, permesso {@code AUDIT_READ_OWN} (il nutrizionista vede
 *       SOLO i propri accessi: {@code utenteId} è forzato lato server, cfr. {@code AuditService.searchOwn}).</li>
 * </ul>
 * IP/email sono dati personali → la vista globale è admin-only.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public PageResponse<AuditLogDto> search(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long utenteId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return auditService.search(clienteId, utenteId, action, toStart(from), toEnd(to), pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('AUDIT_READ_OWN')")
    public PageResponse<AuditLogDto> searchOwn(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return auditService.searchOwn(clienteId, action, toStart(from), toEnd(to), pageable);
    }

    private static Instant toStart(LocalDate d) {
        return d == null ? null : d.atStartOfDay(ZONE).toInstant();
    }

    private static Instant toEnd(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX).atZone(ZONE).toInstant();
    }
}
