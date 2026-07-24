package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.service.CurrentUserService;

/**
 * Identità dell'utente autenticato (sessione/DB), per il frontend cookie-mode (binario Keycloak): sostituisce
 * la decodifica client-side del JWT. Ruolo + permessi arrivano dalla <b>DB</b> (non dal token), coerente con
 * "IdP authenticates, DB authorizes". Funziona in entrambi i binari (session-auth keycloak / Bearer legacy).
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public MeResponse me() {
        Utente u = currentUserService.getMe();
        String ruolo = (u.getRuolo() != null) ? u.getRuolo().getAlias() : null;
        List<String> permessi = (u.getRuolo() != null && u.getRuolo().getRuoloPermessi() != null)
                ? u.getRuolo().getRuoloPermessi().stream().map(rp -> rp.getPermesso().getAlias()).toList()
                : List.of();
        return new MeResponse(u.getId(), u.getEmail(), u.getNome(), u.getCognome(), ruolo, permessi);
    }

    /** Vista minimale dell'identità corrente per il FE. */
    public record MeResponse(Long id, String email, String nome, String cognome, String ruolo, List<String> permessi) {}
}
