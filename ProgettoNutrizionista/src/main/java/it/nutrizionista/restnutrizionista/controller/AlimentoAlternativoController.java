package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoFormDto;
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
}
