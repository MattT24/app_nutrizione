package it.nutrizionista.restnutrizionista.security;

/**
 * Errore tecnico nella cancellazione dell'identità Keycloak via Admin API — I10, erasure cross-store (art. 17).
 * <p>NON è user-facing: il chiamante ({@code UtenteService.deleteAccount}) la cattura <b>best-effort</b> (l'erasure
 * DB è già committata; il reconcile sweep — I10 Fase 2 — recupera l'eventuale identità orfana). Un <b>404</b>
 * dell'Admin API NON genera questa eccezione (utente già assente = successo idempotente).
 */
public class KeycloakAdminException extends RuntimeException {

    public KeycloakAdminException(String message) {
        super(message);
    }

    public KeycloakAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
