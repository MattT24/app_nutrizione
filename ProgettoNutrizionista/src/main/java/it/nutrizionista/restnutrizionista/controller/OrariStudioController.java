package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Recupera gli orari studio del nutrizionista autenticato.

     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ORARI_STUDIO_READ')")
    public ResponseEntity<OrariStudioDto> getMe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var dto = service.getOrariStudioMe(email);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('ORARI_STUDIO_UPDATE')")
    public ResponseEntity<OrariStudioDto> upsertMe(@Valid @RequestBody OrariStudioFormDto form) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var saved = service.upsertOrariStudioMe(email, form);
        return ResponseEntity.status(201).body(saved);
    }
}
