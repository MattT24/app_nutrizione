package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import it.nutrizionista.restnutrizionista.dto.OrariStudioDto;
import it.nutrizionista.restnutrizionista.dto.OrariStudioFormDto;
import it.nutrizionista.restnutrizionista.service.OrariStudioService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/orari_studio")
public class OrariStudioController {

    @Autowired
    private OrariStudioService service;

    /**
     * Recupera TUTTI gli orari studio (i 7 giorni) del nutrizionista autenticato.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ORARI_STUDIO_READ')")
    public ResponseEntity<List<OrariStudioDto>> getMe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<OrariStudioDto> dtoList = service.getOrariStudioMe(email);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Salva o aggiorna un SINGOLO giorno
     */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('ORARI_STUDIO_UPDATE')")
    public ResponseEntity<OrariStudioDto> upsertMe(@Valid @RequestBody OrariStudioFormDto form) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        OrariStudioDto saved = service.upsertOrariStudioMe(email, form);
        return ResponseEntity.status(201).body(saved);
    }

    /**
     * Salva o aggiorna l'INTERA SETTIMANA in un colpo solo (ideale per Angular)
     */
    @PutMapping("/me/settimana")
    @PreAuthorize("hasAuthority('ORARI_STUDIO_UPDATE')")
    public ResponseEntity<List<OrariStudioDto>> upsertSettimanaMe(@Valid @RequestBody List<OrariStudioFormDto> formList) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<OrariStudioDto> savedList = service.upsertOrariStudioSettimana(email, formList);
        return ResponseEntity.status(201).body(savedList);
    }

    /**
     * Elimina un orario studio per id
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORARI_STUDIO_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteOrariStudio(id);
        return ResponseEntity.noContent().build();
    }
}