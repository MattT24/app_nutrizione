package it.nutrizionista.restnutrizionista.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Guard anti-regressione del landing role-aware post-login OIDC (nota Code): il super-admin deve atterrare sul
 * <b>control-plane</b> ({@code /admin}), tutti gli altri sulla {@code /home}. Il bug che chiude: il vecchio
 * {@code defaultSuccessUrl("/home", true)} mandava SEMPRE su {@code /home}, bypassando la login screen dove viveva
 * l'unico redirect role-aware (FE) → il super-admin non arrivava mai alla dashboard. La decisione ora sta lato BE,
 * sulle authority costruite dai permessi DB (match esatto {@code SUPER_ADMIN}, come il gate {@code /api/admin/**}).
 * Il MockMvc non esegue il redirect reale post-OIDC → questo test unita rende la fix "guardata dalla suite".
 */
class KeycloakSuccessHandlerTest {

    private final AuthenticationSuccessHandler handler = KeycloakSecurityConfig.roleAwareSuccessHandler();

    @Test
    void superAdmin_redirectsToControlPlane() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/keycloak");
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Authority nuda SUPER_ADMIN come la costruisce AuthorityBuilder dai permessi DB (dual permesso+ruolo).
        var auth = new TestingAuthenticationToken("07efb709-sub", null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"), new SimpleGrantedAuthority("AUDIT_READ")));

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin");
    }

    @Test
    void nutrizionista_redirectsToHome() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/keycloak");
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Permessi tipici del NUTRIZIONISTA: nessuna authority SUPER_ADMIN.
        var auth = new TestingAuthenticationToken("nutri-sub", null,
                List.of(new SimpleGrantedAuthority("ALIMENTO_READ"), new SimpleGrantedAuthority("CLIENTE_UPDATE")));

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/home");
    }

    @Test
    void noAuthorities_redirectsToHome() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/keycloak");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var auth = new TestingAuthenticationToken("nobody-sub", null, List.of());

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/home");
    }
}
