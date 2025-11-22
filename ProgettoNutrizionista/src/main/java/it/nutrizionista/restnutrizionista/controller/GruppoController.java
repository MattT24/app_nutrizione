package it.nutrizionista.restnutrizionista.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import it.nutrizionista.restnutrizionista.dto.GruppoDto;
import it.nutrizionista.restnutrizionista.dto.GruppoFormDto;
import it.nutrizionista.restnutrizionista.dto.GruppoPermessiRequest;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.GruppoService;

import java.util.List;

/** API REST per Gruppi. */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/gruppi")
public class GruppoController {

    @Autowired private GruppoService service;

    /** Crea gruppo. */
    @PostMapping
    @PreAuthorize("hasAuthority('GRUPPO_CREATE')")
    public ResponseEntity<GruppoDto> create(@Valid @RequestBody GruppoFormDto form) {
    	var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }

    /** Aggiorna gruppo (id nel body). */
    @PutMapping
    @PreAuthorize("hasAuthority('GRUPPO_UPDATE')")
    public ResponseEntity<GruppoDto> update(@Valid @RequestBody GruppoFormDto form) {
    	var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

    /** Elimina gruppo (id nel body). */
    @DeleteMapping
    @PreAuthorize("hasAuthority('GRUPPO_DELETE')")
    public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
        service.delete(req.getId());
        return ResponseEntity.noContent().build();
    }

    /** Dettaglio gruppo (withPermessi=true include i permessi). */
    @GetMapping("/byId")
    @PreAuthorize("hasAuthority('GRUPPO_READ')")
    public ResponseEntity<GruppoDto> get(@RequestBody IdRequest req,
                         @RequestParam(defaultValue = "false") boolean withPermessi) {
    	var dto = service.getById(req.getId(), withPermessi);
        return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    /** Lista paginata gruppi. */
    @GetMapping
    @PreAuthorize("hasAuthority('GRUPPO_READ')")
    public PageResponse<GruppoDto> list(Pageable pageable,
                                        @RequestParam(defaultValue = "false") boolean withPermessi) {
        return service.list(pageable, withPermessi);
    }

    /** Lista completa (non paginata). */
    @GetMapping("/tutti")
    @PreAuthorize("hasAuthority('GRUPPO_READ')")
    public List<GruppoDto> listAll(@RequestParam(defaultValue = "false") boolean withPermessi) {
        return service.listAll(withPermessi);
    }

    /** Associa permessi ad un gruppo. */
    @PostMapping("/associa-permessi")
    @PreAuthorize("hasAuthority('GRUPPO_UPDATE')")
    public ResponseEntity<GruppoDto> associaPermessi(@Valid @RequestBody GruppoPermessiRequest req) {
    	var create = service.associaPermessi(req);
    	return ResponseEntity.status(201).body(create);
    }

    /** Dissocia permessi da un gruppo. */
    @PostMapping("/dissocia-permessi")
    @PreAuthorize("hasAuthority('GRUPPO_UPDATE')")
    public ResponseEntity<GruppoDto> dissociaPermessi(@Valid @RequestBody GruppoPermessiRequest req) {
    	var create = service.dissociaPermessi(req);
    	return ResponseEntity.status(201).body(create);
    }
}
