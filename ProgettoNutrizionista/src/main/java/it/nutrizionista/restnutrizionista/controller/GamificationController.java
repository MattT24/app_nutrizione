package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.GamificationEventoDto;
import it.nutrizionista.restnutrizionista.dto.GamificationStatoDto;
import it.nutrizionista.restnutrizionista.service.GamificationService;

/**
 * API REST gamification per il nutrizionista (punti, livello, badge). Nessun endpoint di
 * scrittura: i punti si generano solo lato server in risposta ad azioni reali già autenticate.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    @Autowired private GamificationService service;

    /** Stato gamification (punti, livello, badge) del nutrizionista loggato. */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public GamificationStatoDto me() {
        return service.getStatoPerMe();
    }

    /** Storico eventi punti del nutrizionista loggato (default 20). */
    @GetMapping("/storico")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public List<GamificationEventoDto> storico(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        return service.getStorico(limit);
    }

    /** Riscatta un mese gratis di abbonamento spendendo punti riscattabili. */
    @PostMapping("/riscatta-mese-gratis")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public GamificationStatoDto riscattaMeseGratis() {
        return service.riscattaMeseGratis();
    }
}
