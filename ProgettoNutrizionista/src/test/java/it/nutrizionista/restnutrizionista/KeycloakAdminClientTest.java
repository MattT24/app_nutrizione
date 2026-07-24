package it.nutrizionista.restnutrizionista;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;

import org.springframework.http.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import it.nutrizionista.restnutrizionista.security.KeycloakAdminClient;
import it.nutrizionista.restnutrizionista.security.KeycloakAdminException;

/**
 * I10 — unit test del {@link KeycloakAdminClient} (Keycloak Admin API): <b>idempotenza 404</b> = successo,
 * altri errori = {@link KeycloakAdminException}, header {@code Authorization: Bearer} presente. HTTP via
 * {@link MockRestServiceServer} legato a un {@link RestClient}; token client_credentials mockato.
 */
class KeycloakAdminClientTest {

    private static final String BASE = "http://kc.test";
    private static final String REALM = "statera";
    private static final String SUB = "sub-123";
    private static final String URI = BASE + "/admin/realms/" + REALM + "/users/" + SUB;

    private MockRestServiceServer server;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();

        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClient authorized = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "tok-abc", Instant.now(), Instant.now().plusSeconds(60));
        when(authorized.getAccessToken()).thenReturn(token);
        when(manager.authorize(any())).thenReturn(authorized);

        client = new KeycloakAdminClient(manager, rest, REALM, "statera-bff", "http://localhost:4200/");
    }

    @Test
    void deleteUser_204_ok() {
        server.expect(requestTo(URI))
              .andExpect(method(HttpMethod.DELETE))
              .andExpect(header("Authorization", "Bearer tok-abc"))
              .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> client.deleteUser(SUB));
        server.verify();
    }

    @Test
    void deleteUser_404_idempotenteSuccesso() {
        server.expect(requestTo(URI))
              .andExpect(method(HttpMethod.DELETE))
              .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertDoesNotThrow(() -> client.deleteUser(SUB)); // 404 = utente già assente = successo idempotente
        server.verify();
    }

    @Test
    void deleteUser_500_lanciaKeycloakAdminException() {
        server.expect(requestTo(URI))
              .andExpect(method(HttpMethod.DELETE))
              .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(KeycloakAdminException.class, () -> client.deleteUser(SUB));
        server.verify();
    }

    // ─────────── Onboarding: createUser (POST + invito execute-actions-email) ───────────

    @Test
    void createUser_201_inviaInvitoExecuteActionsEmail() {
        String usersUri = BASE + "/admin/realms/" + REALM + "/users";
        server.expect(requestTo(usersUri))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("Authorization", "Bearer tok-abc"))
              .andRespond(withStatus(HttpStatus.CREATED).location(java.net.URI.create(usersUri + "/kc-999")));
        server.expect(method(HttpMethod.PUT))
              .andExpect(requestTo(containsString("/users/kc-999/execute-actions-email")))
              .andExpect(queryParam("client_id", "statera-bff"))       // redirect invito → app
              .andExpect(header("Authorization", "Bearer tok-abc"))
              .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> client.createUser("anna@studio.it", "Anna", "Rossi"));
        server.verify();
    }

    @Test
    void createUser_409_reinvitoIdempotente_lookupPoiInvito() {
        String usersUri = BASE + "/admin/realms/" + REALM + "/users";
        server.expect(requestTo(usersUri))
              .andExpect(method(HttpMethod.POST))
              .andRespond(withStatus(HttpStatus.CONFLICT));                 // già esistente
        server.expect(method(HttpMethod.GET))
              .andExpect(requestTo(containsString("/admin/realms/" + REALM + "/users")))
              .andExpect(queryParam("email", "anna@studio.it"))
              .andExpect(queryParam("exact", "true"))
              .andRespond(withSuccess("[{\"id\":\"kc-777\"}]", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.PUT))
              .andExpect(requestTo(containsString("/users/kc-777/execute-actions-email")))
              .andExpect(queryParam("client_id", "statera-bff"))
              .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> client.createUser("anna@studio.it", "Anna", "Rossi"));
        server.verify();
    }
}
