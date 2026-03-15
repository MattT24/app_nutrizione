package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.PastoTemplateDto;
import it.nutrizionista.restnutrizionista.dto.RicettaDto;
import it.nutrizionista.restnutrizionista.service.RicettaService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/ricette")
public class RicettaController {

    @Autowired private RicettaService service;

    /** Lista tutte le ricette pubbliche con ingredienti e macro calcolati. */
    @GetMapping
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<List<RicettaDto>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    /** Dettaglio singola ricetta. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<RicettaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * Importa la ricetta come PastoTemplate personale del nutrizionista loggato.
     * Restituisce il PastoTemplateDto appena creato, pronto per essere aggiunto
     * localmente alla lista dei template in frontend.
     */
    @PostMapping("/{id}/import-template")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<PastoTemplateDto> importAsTemplate(@PathVariable Long id) {
        PastoTemplateDto created = service.importAsTemplate(id);
        return ResponseEntity.status(201).body(created);
    }
}
