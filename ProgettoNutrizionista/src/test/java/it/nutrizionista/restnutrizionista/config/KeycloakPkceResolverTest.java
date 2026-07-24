package it.nutrizionista.restnutrizionista.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Guard anti-regressione del PKCE per il client CONFIDENTIAL (nota Code): il resolver di
 * {@link KeycloakSecurityConfig} DEVE aggiungere {@code code_challenge} + {@code code_challenge_method=S256} alla
 * authorization request. Senza, Keycloak (`statera-bff`, PKCE richiesto) rifiuta con
 * "Missing parameter: code_challenge_method". Questo trasforma "verificato una volta live" in "guardato dalla suite"
 * (il MockMvc non fa il redirect reale, quindi non lo copre).
 */
class KeycloakPkceResolverTest {

    private ClientRegistrationRepository repo() {
        ClientRegistration reg = ClientRegistration.withRegistrationId("keycloak")
                .clientId("statera-bff")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/keycloak")
                .scope("openid", "profile", "email")
                .authorizationUri("http://localhost:8081/realms/statera/protocol/openid-connect/auth")
                .tokenUri("http://localhost:8081/realms/statera/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:8081/realms/statera/protocol/openid-connect/certs")
                .userInfoUri("http://localhost:8081/realms/statera/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub")
                .issuerUri("http://localhost:8081/realms/statera")
                .build();
        return new InMemoryClientRegistrationRepository(reg);
    }

    @Test
    void resolver_addsPkceChallenge_forConfidentialClient() {
        OAuth2AuthorizationRequestResolver resolver = KeycloakSecurityConfig.pkceResolver(repo());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/keycloak");

        OAuth2AuthorizationRequest authReq = resolver.resolve(request);

        assertThat(authReq).as("il resolver deve risolvere la authorization request per /oauth2/authorization/keycloak").isNotNull();
        assertThat(authReq.getAdditionalParameters()).containsKey("code_challenge");
        assertThat(authReq.getAdditionalParameters()).containsEntry("code_challenge_method", "S256");
    }
}
