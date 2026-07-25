package it.nutrizionista.restnutrizionista.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.security.AuthorityBuilder;
import it.nutrizionista.restnutrizionista.security.KeycloakUserLinkService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Binario di autenticazione <b>Keycloak</b> (BFF OIDC) — attivo con {@code auth.provider=keycloak}.
 * Mutuamente esclusivo con {@link LegacySecurityConfig}. In questo binario il custom-JWT è dismesso.
 *
 * <p><b>Due filter chain</b> ("IdP authenticates, DB authorizes"):
 * <ul>
 *   <li><b>bearer</b> {@code @Order(0)}, {@code /api-mobile/**}: resource-server JWT (JWKS Keycloak), stateless,
 *       CSRF off — forward-looking per il <b>mobile paziente</b>. ⚠️ mai cookie di sessione.</li>
 *   <li><b>browser</b> {@code @Order(1)}: {@code oauth2Login} (cookie di sessione), CSRF SPA, RP-initiated +
 *       Back-Channel Logout.</li>
 * </ul>
 * In <b>entrambe</b> le authorities arrivano dai <b>permessi in DB</b> (via {@link AuthorityBuilder}), non dal token;
 * l'identità è linkata all'{@code Utente} via {@link KeycloakUserLinkService} (gate {@code email_verified},
 * niente auto-provisioning). Il principal-name è il {@code sub} → coerente con {@code CurrentUserService.getMe()}.
 */
@Configuration
@ConditionalOnProperty(name = "auth.provider", havingValue = "keycloak")
public class KeycloakSecurityConfig {

    // ───────────────────────── Chain BEARER (mobile) — @Order(0) ─────────────────────────
    @Bean
    @Order(0)
    public SecurityFilterChain bearerFilterChain(HttpSecurity http,
            @Qualifier("jwtAuthConverter") Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter) throws Exception {
        http
            .securityMatcher("/api-mobile/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthConverter)));
        return http.build();
    }

    // ───────────────────────── Chain BROWSER (SPA/BFF) — @Order(1) ─────────────────────────
    @Bean
    @Order(1)
    public SecurityFilterChain browserFilterChain(HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .cors(c -> c.configurationSource(corsSource))
            // CSRF SPA (double-submit Angular): cookie XSRF-TOKEN (leggibile da JS) + header X-XSRF-TOKEN.
            // Il CsrfCookieFilter materializza il token (deferred loading) così il cookie è sempre emesso.
            // NB: `.csrf(csrf -> csrf.spa())` NON è disponibile su questa versione di Spring Security (verificato:
            // CsrfConfigurer non espone spa()); questa è la ricetta manuale equivalente. Migrare a .spa() (che aggiunge
            // BREACH/XOR) su un futuro upgrade di Spring Security che lo esponga.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/uploads/loghi/**").permitAll()
                .requestMatchers(
                        "/oauth2/**", "/login/**", "/logout/**",
                        "/v3/api-docs", "/v3/api-docs/**",
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/swagger-resources", "/swagger-resources/**",
                        "/configuration/ui", "/configuration/security", "/webjars/**"
                    ).permitAll()
                // Difesa in profondità (invariata): admin richiede SUPER_ADMIN già a livello di chain.
                .requestMatchers("/api/admin/**").hasAuthority("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(login -> login
                // PKCE anche per il client confidential (Spring non lo invia di default → Keycloak `statera-bff` lo esige).
                .authorizationEndpoint(ae -> ae.authorizationRequestResolver(pkceResolver(clientRegistrationRepository)))
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                // Post-login: landing role-aware lato BE (punto decisionale autorevole post-OIDC) → SUPER_ADMIN su
                // /admin, tutti gli altri su /home (SPA fallback + authGuard). NON defaultSuccessUrl("/home") fisso:
                // quello bypassa la login screen (dove viveva il redirect role-aware FE) → il super-admin atterrava
                // sempre su /home. Ora la decisione sta dove ci sono le authority DB, in parità col landing legacy.
                .successHandler(roleAwareSuccessHandler()))
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))
            // Back-Channel Logout: Keycloak → BFF invalida la sessione (revoca propagata, chiude il "logout non effettivo").
            .oidcLogout(oidc -> oidc.backChannel(Customizer.withDefaults()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    // ───────────────────────── User services (authorities dalla DB) ─────────────────────────

    /** Chain browser: dopo il login OIDC, linka l'identità e costruisce le authorities dai permessi DB. */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
            KeycloakUserLinkService linkService, AuthorityBuilder authorityBuilder) {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            OidcUser oidc = delegate.loadUser(userRequest);
            Utente u = linkService.linkOrReject(
                    oidc.getSubject(), oidc.getEmail(), Boolean.TRUE.equals(oidc.getEmailVerified()));
            // name attribute = "sub" → principal-name coerente con CurrentUserService.getMe() (subjectId)
            return new DefaultOidcUser(authorityBuilder.build(u), oidc.getIdToken(), oidc.getUserInfo(), "sub");
        };
    }

    /** Chain bearer: valida il JWT Keycloak (JWKS) e costruisce le authorities dai permessi DB. */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter(
            KeycloakUserLinkService linkService, AuthorityBuilder authorityBuilder) {
        return jwt -> {
            Utente u = linkService.linkOrReject(
                    jwt.getSubject(), jwt.getClaimAsString("email"), Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
            // principal-name = sub
            return new JwtAuthenticationToken(jwt, authorityBuilder.build(u), jwt.getSubject());
        };
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repo) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(repo);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }

    /**
     * Abilita PKCE (S256) anche per il client <b>CONFIDENTIAL</b>: Spring Security invia PKCE di default solo per i
     * client public, ma il client Keycloak {@code statera-bff} lo richiede (design "Authorization Code + PKCE").
     * Senza, il redirect a Keycloak parte senza {@code code_challenge} → {@code invalid_request: Missing parameter:
     * code_challenge_method}. Package-private static → testabile a unità ({@code KeycloakPkceResolverTest}).
     */
    static OAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository repo) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    /**
     * Landing <b>role-aware</b> post-login OIDC: {@code SUPER_ADMIN → /admin} (control-plane), tutti gli altri
     * {@code → /home}. Fonte unica = le <b>authority</b> dell'{@link org.springframework.security.core.Authentication}
     * (costruite dai <b>permessi DB</b> via {@link AuthorityBuilder}, mai dal token) → parità col landing legacy
     * ({@code login()} FE). {@code SUPER_ADMIN} è authority nuda (match esatto, come {@code hasAuthority} sul gate
     * admin). Ignora il {@code SavedRequest} (nessun bounce su una {@code /api/**} salvata), come l'ex
     * {@code defaultSuccessUrl(url, true)}. Package-private static → testabile a unità.
     */
    static AuthenticationSuccessHandler roleAwareSuccessHandler() {
        DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
        return (request, response, authentication) -> {
            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "SUPER_ADMIN".equals(a.getAuthority()));
            redirectStrategy.sendRedirect(request, response, isSuperAdmin ? "/admin" : "/home");
        };
    }

    /**
     * Materializza il {@link CsrfToken} (deferred) su ogni richiesta → il cookie {@code XSRF-TOKEN} viene sempre
     * emesso, così Angular può rileggerlo e rimandarlo in {@code X-XSRF-TOKEN} (double-submit).
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
