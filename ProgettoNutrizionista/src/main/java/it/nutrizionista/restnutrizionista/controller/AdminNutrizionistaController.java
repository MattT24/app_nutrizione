package it.nutrizionista.restnutrizionista.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.CreaNutrizionistaRequest;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.service.AdminNutrizionistaService;
import jakarta.validation.Valid;

/**
 * Onboarding admin-invite di un nutrizionista (beta Keycloak): l'admin provisiona l'account, Keycloak invia l'invito
 * email (set-password + verify). Self-registration pubblica DISABILITATA (invariante I2). Doppio gate (difesa in
 * profondità): {@code @PreAuthorize} + {@code /api/admin/**} in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/nutrizionisti")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminNutrizionistaController {

    private final AdminNutrizionistaService service;

    public AdminNutrizionistaController(AdminNutrizionistaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UtenteDto> invita(@Valid @RequestBody CreaNutrizionistaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invita(req));
    }

    /**
     * Invariante E (a) — erasure control-plane (art. 17): l'UNICA via admin per cancellare un account. Doppio gate
     * (SUPER_ADMIN via `@PreAuthorize` di classe + `/api/admin/**`); il service rifiuta target SUPER_ADMIN e delega alla
     * foglia canonica (`UtenteService.delete` → `deleteAccount` → hook I10 cross-store Keycloak).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> elimina(@PathVariable Long id) {
        service.elimina(id);
        return ResponseEntity.noContent().build();
    }
}
