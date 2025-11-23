package it.nutrizionista.restnutrizionista.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PermessoDto;
import it.nutrizionista.restnutrizionista.dto.PermessoFormDto;
import it.nutrizionista.restnutrizionista.service.PermessoService;

import java.util.List;

/** API REST per Permessi. */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/permessi")
public class PermessoController {

    @Autowired private PermessoService service;

    /** Crea permesso. */
    @PostMapping
    @PreAuthorize("hasAuthority('PERMESSO_CREATE')")
    public ResponseEntity<PermessoDto> create(@Valid @RequestBody PermessoFormDto form) {
    	var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }

    /** Aggiorna permesso (id nel body). */
    @PutMapping
    @PreAuthorize("hasAuthority('PERMESSO_UPDATE')")
    public ResponseEntity<PermessoDto> update(@Valid @RequestBody PermessoFormDto form) {
    	var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

    /** Elimina permesso (id nel body). */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERMESSO_DELETE')")
    public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
        service.delete(req.getId());
        return ResponseEntity.noContent().build();
    }

    /** Dettaglio permesso. */
    @GetMapping("/byId")
    @PreAuthorize("hasAuthority('PERMESSO_READ')")
    public ResponseEntity<PermessoDto> get(@RequestBody IdRequest req) {
    	var dto = service.getById(req.getId());
        return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    /** Lista paginata permessi. */
    @GetMapping
    @PreAuthorize("hasAuthority('PERMESSO_READ')")
    public PageResponse<PermessoDto> list(Pageable pageable) {
        return service.list(pageable);
    }

    /** Lista completa (non paginata). */
    @GetMapping("/tutti")
    @PreAuthorize("hasAuthority('PERMESSO_READ')")
    public List<PermessoDto> listAll() {
        return service.listAll();
    }
}
