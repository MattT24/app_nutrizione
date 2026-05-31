package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AttivitaRecenteDto;
import it.nutrizionista.restnutrizionista.dto.AttivitaTrackRequest;
import it.nutrizionista.restnutrizionista.service.AttivitaRecenteService;
import jakarta.validation.Valid;

/** API REST per le attività recenti del nutrizionista (widget "Ultime attività"). */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/attivita")
public class AttivitaRecenteController {

    @Autowired private AttivitaRecenteService service;

    /** Registra (upsert) un'attività su un cliente. */
    @PostMapping("/track")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public ResponseEntity<Void> track(@Valid @RequestBody AttivitaTrackRequest req) {
        service.track(req);
        return ResponseEntity.noContent().build();
    }

    /** Ultime attività del nutrizionista loggato (default 5). */
    @GetMapping("/recenti")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public List<AttivitaRecenteDto> recenti(@RequestParam(name = "limit", defaultValue = "5") int limit) {
        return service.getRecenti(limit);
    }
}
