package it.nutrizionista.restnutrizionista.security;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client verso la <b>Keycloak Admin REST API</b> per l'erasure CROSS-STORE (I10, art. 17): cancella l'identità
 * dell'utente sull'IdP quando il suo account viene eliminato dal nostro store. Solo Spring nativo
 * ({@link RestClient} + OAuth2 {@code client_credentials} via {@link OAuth2AuthorizedClientManager}) —
 * <b>nessun</b> adapter keycloak-admin (vincolo di progetto).
 *
 * <p>Istanziato SOLO in keycloak-mode ({@code config/KeycloakAdminConfig}, gated {@code auth.provider=keycloak});
 * in legacy il bean non esiste. Invarianti I10: <b>idempotente</b> (404 = già cancellato = successo),
 * token client_credentials cachato/rinnovato da Spring; il chiamante tratta ogni errore <b>best-effort</b> (un
 * fallimento KC NON deve bloccare l'erasure DB). ⚠️ Il {@link RestClient} DEVE avere <b>timeout bounded</b> —
 * l'hook gira sincrono nel thread di richiesta dopo il commit, una KC appesa bloccherebbe la cancellazione
 * (vedi {@code KeycloakAdminConfig}).
 */
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);
    private static final String REGISTRATION_ID = "keycloak-admin";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final RestClient rest;
    private final String realm;
    private final String inviteClientId;    // client per cui KC costruisce il link d'invito (BFF)
    private final String inviteRedirectUri;  // dove atterra dopo set-password+verify (l'app → SSO → gate Wave-3)

    public KeycloakAdminClient(OAuth2AuthorizedClientManager authorizedClientManager, RestClient rest, String realm,
            String inviteClientId, String inviteRedirectUri) {
        this.authorizedClientManager = authorizedClientManager;
        this.rest = rest;
        this.realm = realm;
        this.inviteClientId = inviteClientId;
        this.inviteRedirectUri = inviteRedirectUri;
    }

    /**
     * {@code DELETE /admin/realms/{realm}/users/{subjectId}}. <b>404 = utente già assente = successo idempotente</b>
     * (swallow). Altri errori (rete/timeout/4xx-5xx) → {@link KeycloakAdminException} (il chiamante la tratta
     * best-effort). Keycloak cancella in cascata anche i <b>link federati</b> (es. Google) dell'utente → nessuna
     * chiamata extra necessaria.
     */
    public void deleteUser(String subjectId) {
        String token = obtainAccessToken();
        try {
            rest.delete()
                .uri("/admin/realms/{realm}/users/{id}", realm, subjectId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    // idempotente: identità già assente sull'IdP → successo (nessun throw)
                    log.debug("I10: utente Keycloak sub={} già assente (404) — no-op idempotente", subjectId);
                })
                .toBodilessEntity();
        } catch (RestClientException e) {
            throw new KeycloakAdminException("DELETE utente Keycloak fallita (sub=" + subjectId + ")", e);
        }
    }

    /**
     * <b>Onboarding admin-invite</b>: crea l'utente su Keycloak <b>senza password</b> e invia l'invito email
     * ({@code UPDATE_PASSWORD} + {@code VERIFY_EMAIL}). <b>Idempotente</b>: se l'utente esiste già (409) recupera l'id
     * per email e <b>reinvia</b> l'invito → un re-POST admin funge da re-invito. Errori → {@link KeycloakAdminException}
     * (il chiamante persiste comunque l'{@code Utente} DB e risponde 502 "ripeti l'invito"). {@code username=email}.
     */
    public void createUser(String email, String firstName, String lastName) {
        String token = obtainAccessToken();
        String userId;
        try {
            ResponseEntity<Void> resp = rest.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", email, "email", email, "enabled", true,
                            "firstName", firstName, "lastName", lastName))
                    .retrieve()
                    .toBodilessEntity();
            URI location = resp.getHeaders().getLocation();
            if (location == null) {
                throw new KeycloakAdminException("Location assente nella creazione utente Keycloak (email=" + email + ")");
            }
            String path = location.getPath();
            userId = path.substring(path.lastIndexOf('/') + 1);
        } catch (HttpClientErrorException.Conflict conflict) {
            // 409 = utente già esistente → reinvito idempotente: recupera l'id per email e prosegui con l'invito.
            userId = findUserIdByEmail(email, token);
        } catch (RestClientException e) {
            throw new KeycloakAdminException("POST utente Keycloak fallita (email=" + email + ")", e);
        }
        sendInviteEmail(userId, token);
    }

    /**
     * Invia (o reinvia) l'email di invito con le required-action UPDATE_PASSWORD + VERIFY_EMAIL.
     * ⚠️ `client_id`+`redirect_uri` obbligatori: senza, KC reindirizza alla console account di default; con, l'utente
     * atterra sull'app (`redirect_uri` = valid-redirect del client BFF) → SSO → gate Wave-3.
     */
    private void sendInviteEmail(String userId, String token) {
        try {
            rest.put()
                .uri(b -> b.path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                        .queryParam("client_id", inviteClientId)
                        .queryParam("redirect_uri", inviteRedirectUri)
                        .build(realm, userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException e) {
            throw new KeycloakAdminException("execute-actions-email (invito) fallita (id=" + userId + ")", e);
        }
    }

    /** Cerca l'id dell'utente Keycloak per email esatta (per il reinvito idempotente sul 409). */
    private String findUserIdByEmail(String email, String token) {
        List<Map<String, Object>> users = rest.get()
                .uri(b -> b.path("/admin/realms/{realm}/users")
                        .queryParam("email", email).queryParam("exact", true).build(realm))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        if (users == null || users.isEmpty()) {
            throw new KeycloakAdminException("Utente Keycloak non trovato per email=" + email);
        }
        return String.valueOf(users.get(0).get("id"));
    }

    /** Token del service-account (client_credentials); Spring lo cache/rinnova. */
    private String obtainAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal(REGISTRATION_ID)
                .build();
        OAuth2AuthorizedClient client = authorizedClientManager.authorize(request);
        if (client == null || client.getAccessToken() == null) {
            throw new KeycloakAdminException(
                    "client_credentials: token Keycloak Admin non ottenuto (registration '" + REGISTRATION_ID + "')");
        }
        return client.getAccessToken().getTokenValue();
    }
}
