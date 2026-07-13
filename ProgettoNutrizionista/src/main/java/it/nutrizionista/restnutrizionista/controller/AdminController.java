package it.nutrizionista.restnutrizionista.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.AdminNutrizionistaDto;
import it.nutrizionista.restnutrizionista.dto.AdminStatsDto;
import it.nutrizionista.restnutrizionista.service.AdminService;

import java.util.List;

/**
 * Endpoint riservati al super admin.
 * Difesa in profondità: oltre a @PreAuthorize qui, /api/admin/** è vincolato
 * a SUPER_ADMIN anche a livello di SecurityFilterChain (SecurityConfig).
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminController {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT.admin");

    @Autowired private AdminService adminService;

    @GetMapping("/nutrizionisti/stats")
    public ResponseEntity<AdminStatsDto> stats(Authentication auth) {
        audit.info("Accesso admin stats da {}", auth.getName());
        return ResponseEntity.ok(adminService.stats());
    }

    @GetMapping("/nutrizionisti")
    public ResponseEntity<List<AdminNutrizionistaDto>> nutrizionisti(Authentication auth) {
        audit.info("Accesso admin lista nutrizionisti da {}", auth.getName());
        return ResponseEntity.ok(adminService.nutrizionisti());
    }
}
