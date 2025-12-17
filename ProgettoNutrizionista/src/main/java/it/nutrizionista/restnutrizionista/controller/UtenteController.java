package it.nutrizionista.restnutrizionista.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.LogoRequestDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.dto.UtenteFormDto;
import it.nutrizionista.restnutrizionista.dto.UtenteProfileUpdateDto;
import it.nutrizionista.restnutrizionista.service.UtenteService;

import java.io.IOException;
import java.util.List;

/** API REST per Utenti. */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    @Autowired private UtenteService service;

    /** Crea utente. */
    @PostMapping
    @PreAuthorize("hasAuthority('UTENTE_CREATE')")
    public ResponseEntity<UtenteDto> create(@Valid @RequestBody UtenteFormDto form) {
    	var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }

    /** Aggiorna utente (id nel body). */
    @PutMapping
    @PreAuthorize("hasAuthority('UTENTE_UPDATE')")
    public ResponseEntity<UtenteDto> update(@Valid @RequestBody UtenteFormDto form) {
    	var updated = service.update(form);
    	return ResponseEntity.status(201).body(updated);
    }

    /** Elimina utente (id nel body). */
    @DeleteMapping
    @PreAuthorize("hasAuthority('UTENTE_DELETE')")
    public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
        service.delete(req.getId());
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/profilo")
    @PreAuthorize("hasAuthority('UTENTE_DELETE_PROFILE')")
    public ResponseEntity<Void> deleteMyProfile() {
        service.deleteMyProfile();
        return ResponseEntity.noContent().build();
    }

    //Sospendi utente
    
    /** Dettaglio utente. */
    @GetMapping("/byId")
    @PreAuthorize("hasAuthority('UTENTE_READ')")
    public ResponseEntity<UtenteDto> get(@RequestBody IdRequest req) {
    	var dto = service.getById(req.getId());
    	return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    /** Lista paginata. */
    @GetMapping
    @PreAuthorize("hasAuthority('UTENTE_READ')")
    public PageResponse<UtenteDto> list(Pageable pageable) {
        return service.list(pageable);
    }

    /** Lista completa (non paginata). */
    @GetMapping("/tutti")
    @PreAuthorize("hasAuthority('UTENTE_READ')")
    public List<UtenteDto> listAll() {
        return service.listAll();
    }

    /** Profilo utente corrente (id dal JWT). */
    @GetMapping("/profilo")
    @PreAuthorize("hasAuthority('UTENTE_PROFILE')")
    public ResponseEntity<UtenteDto> profile() {
    	var dto = service.getProfile();
    	return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }
    
  
    @PutMapping("/myprofile")
    @PreAuthorize("hasAuthority('UTENTE_PROFILE')")
    public ResponseEntity<UtenteDto> updateMyProfile(
            @Valid @RequestBody UtenteProfileUpdateDto form
    ) {
        var updated = service.updateMyProfile(form);
        return ResponseEntity.ok(updated);
    }

    
 // Inserimento Logo nutrizionista
    @PostMapping("/logo")
    public ResponseEntity<UtenteDto> uploadLogo(
            @Valid @ModelAttribute LogoRequestDto form
    ) throws IOException {

        var dto = service.updateLogo(form);
        return ResponseEntity.ok(dto);
    }

}