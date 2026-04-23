package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AvversionePersonaleDto;
import it.nutrizionista.restnutrizionista.dto.AvversionePersonaleFormDto;
import it.nutrizionista.restnutrizionista.service.ClienteBlacklistService;
import jakarta.validation.Valid;

/**
 * Controller Sub-Resource dedicato alla gestione della Blacklist alimentare
 * di un singolo Cliente. Mantiene il ClienteController pulito e focalizzato
 * sulle generalità anagrafiche.
 *
 * Pattern REST: /api/clienti/{clienteId}/blacklist
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/clienti/{clienteId}/blacklist")
public class ClienteBlacklistController {

    private final ClienteBlacklistService blacklistService;

    // ── Iniezione Esplicita via Costruttore (NO LOMBOK) ──
    public ClienteBlacklistController(ClienteBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    /**
     * GET /api/clienti/{clienteId}/blacklist
     * Ritorna tutte le avversioni alimentari associate al paziente.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public ResponseEntity<List<AvversionePersonaleDto>> getBlacklist(
            @PathVariable Long clienteId) {
        List<AvversionePersonaleDto> result = blacklistService.getBlacklistByCliente(clienteId);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/clienti/{clienteId}/blacklist
     * Aggiunge un nuovo alimento alla blacklist del paziente.
     * Validazione input tramite @Valid su AvversionePersonaleFormDto.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
    public ResponseEntity<AvversionePersonaleDto> addToBlacklist(
            @PathVariable Long clienteId,
            @Valid @RequestBody AvversionePersonaleFormDto form) {
        AvversionePersonaleDto created = blacklistService.addAlimentoToBlacklist(clienteId, form);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * DELETE /api/clienti/{clienteId}/blacklist/{alimentoId}
     * Rimuove un alimento dalla blacklist tramite il suo ID semantico (alimentoId).
     */
    @DeleteMapping("/{alimentoId}")
    @PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
    public ResponseEntity<Void> removeFromBlacklist(
            @PathVariable Long clienteId,
            @PathVariable Long alimentoId) {
        blacklistService.removeAlimentoFromBlacklist(clienteId, alimentoId);
        return ResponseEntity.noContent().build();
    }
}
