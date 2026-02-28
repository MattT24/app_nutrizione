package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoFormDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoUpsertDto;
import it.nutrizionista.restnutrizionista.service.AlimentoAlternativoService;
import jakarta.validation.Valid;

/**
 * Controller REST per la gestione degli alimenti alternativi
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_alternativi")
public class AlimentoAlternativoController {

    @Autowired
    private AlimentoAlternativoService service;

    /**
     * Crea una nuova alternativa
     * POST /api/alimenti_alternativi
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_CREATE')")
    public ResponseEntity<AlimentoAlternativoDto> create(@Valid @RequestBody AlimentoAlternativoFormDto form) {
        var created = service.create(form);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Aggiorna un'alternativa esistente
     * PUT /api/alimenti_alternativi
     */
    @PutMapping
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_UPDATE')")
    public ResponseEntity<AlimentoAlternativoDto> update(@Valid @RequestBody AlimentoAlternativoFormDto form) {
        var updated = service.update(form);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina un'alternativa
     * DELETE /api/alimenti_alternativi/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ottiene una singola alternativa per ID
     * GET /api/alimenti_alternativi/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_READ')")
    public ResponseEntity<AlimentoAlternativoDto> getById(@PathVariable Long id) {
        var dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Lista tutte le alternative per un alimento in pasto, ordinate per priorità
     * GET /api/alimenti_alternativi/alimento_pasto/{alimentoPastoId}
     */
    @GetMapping("/alimento_pasto/{alimentoPastoId}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_READ')")
    public ResponseEntity<List<AlimentoAlternativoDto>> listByAlimentoPasto(@PathVariable Long alimentoPastoId) {
        return ResponseEntity.ok(service.listByAlimentoPasto(alimentoPastoId));
    }

    // === PER-PASTO ENDPOINTS ===

    /**
     * Lista tutte le alternative per un pasto
     * GET /api/alimenti_alternativi/pasto/{pastoId}
     */
    @GetMapping("/pasto/{pastoId}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_READ')")
    public ResponseEntity<List<AlimentoAlternativoDto>> listByPasto(@PathVariable Long pastoId) {
        return ResponseEntity.ok(service.listByPasto(pastoId));
    }

    /**
     * Crea una nuova alternativa per un pasto
     * POST /api/alimenti_alternativi/pasto/{pastoId}
     */
    @PostMapping("/pasto/{pastoId}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_CREATE')")
    public ResponseEntity<AlimentoAlternativoDto> createForPasto(
            @PathVariable Long pastoId,
            @Valid @RequestBody AlimentoAlternativoUpsertDto body) {
        return ResponseEntity.status(201).body(service.createForPasto(pastoId, body));
    }

    /**
     * Aggiorna un'alternativa per un pasto
     * PUT /api/alimenti_alternativi/pasto/{pastoId}/{alternativeId}
     */
    @PutMapping("/pasto/{pastoId}/{alternativeId}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_UPDATE')")
    public ResponseEntity<AlimentoAlternativoDto> updateForPasto(
            @PathVariable Long pastoId,
            @PathVariable Long alternativeId,
            @Valid @RequestBody AlimentoAlternativoUpsertDto body) {
        return ResponseEntity.ok(service.updateForPasto(pastoId, alternativeId, body));
    }

    /**
     * Elimina un'alternativa per un pasto
     * DELETE /api/alimenti_alternativi/pasto/{pastoId}/{alternativeId}
     */
    @DeleteMapping("/pasto/{pastoId}/{alternativeId}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_DELETE')")
    public ResponseEntity<Void> deleteForPasto(
            @PathVariable Long pastoId,
            @PathVariable Long alternativeId) {
        service.deleteForPasto(pastoId, alternativeId);
        return ResponseEntity.noContent().build();
    }

    // === BATCH PER-SCHEDA ===

    /**
     * Carica tutte le alternative di tutti i pasti di una scheda in una sola chiamata.
     * GET /api/alimenti_alternativi/scheda/{schedaId}
     * → Map<pastoId, List<AlimentoAlternativoDto>>
     */
    @GetMapping("/scheda/{schedaId}")
    @PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_READ')")
    public ResponseEntity<java.util.Map<Long, java.util.List<AlimentoAlternativoDto>>> listByScheda(
            @PathVariable Long schedaId) {
        return ResponseEntity.ok(service.listByScheda(schedaId));
    }
}
