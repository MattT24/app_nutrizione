package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica il binario <b>keycloak</b> ({@code auth.provider=keycloak}) senza rete: il context carica le 2 filter
 * chain e i gate reggono. {@code ClientRegistrationRepository} + {@code JwtDecoder} sono forniti come bean di test
 * (nessuna discovery verso Keycloak). Il MODE (2 chain) è provato dal fatto che il context si avvia con questa
 * config attiva e che ENTRAMBE le chain rispondono (browser: gate SUPER_ADMIN; bearer: richiede auth).
 *
 * <p>NB: {@code oidcLogin()}/{@code jwt()} iniettano l'authentication → verificano le REGOLE della chain, non il
 * converter reale (link + authorities dalla DB), coperto dagli unit test {@code KeycloakUserLinkServiceTest}/
 * {@code AuthorityBuilderTest}. Il flusso OIDC end-to-end resta verifica LIVE (browser + Keycloak reale).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.provider=keycloak")
@Import(KeycloakSecurityConfigMockMvcTest.KeycloakTestBeans.class)
class KeycloakSecurityConfigMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    // ── chain browser (@Order 1): gate /api/admin/** su SUPER_ADMIN ──
    @Test
    void adminEndpoint_authenticatedSenzaSuperAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/ping").with(oidcLogin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_conSuperAdmin_nonForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/ping")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("SUPER_ADMIN"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    // ── chain bearer (@Order 0): /api-mobile/** richiede autenticazione (JWT), mai cookie ──
    @Test
    void mobileEndpoint_senzaToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api-mobile/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mobileEndpoint_conJwt_nonUnauthorized() throws Exception {
        mockMvc.perform(get("/api-mobile/ping").with(jwt()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    /** Bean necessari all'avvio del binario keycloak, senza discovery di rete. */
    @TestConfiguration
    static class KeycloakTestBeans {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
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

        @Bean
        JwtDecoder jwtDecoder() {
            // Mock: la chain bearer richiede il bean all'avvio; con il postprocessor jwt() non viene invocato.
            return mock(JwtDecoder.class);
        }
    }
}
