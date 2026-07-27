package it.nutrizionista.restnutrizionista.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import it.nutrizionista.restnutrizionista.security.JwtAuthFilter;

/**
 * Binario di autenticazione <b>legacy</b> (custom-JWT HS256): JWT stateless via {@link JwtAuthFilter} +
 * {@code @PreAuthorize}. È il comportamento storico, INVARIATO (spostato qui da {@code SecurityConfig}).
 *
 * <p>Attivo quando {@code auth.provider=legacy-jwt} <b>o assente</b> ({@code matchIfMissing=true}) → default e
 * modalità di tutti i test esistenti (zero regressione). I bean condivisi (CORS/encoder/authManager) stanno in
 * {@link SecurityBeansConfig}. La selezione errata del flag è intercettata da {@link AuthProviderStartupValidator}.
 */
@Configuration
@ConditionalOnProperty(name = "auth.provider", havingValue = "legacy-jwt", matchIfMissing = true)
public class LegacySecurityConfig {

    /** B6 — CSP in report-only (rollout) vs enforce, da property (default: enforce). */
    @Value("${app.security.csp.report-only:false}")
    private boolean cspReportOnly;

    /**
     * B6 — CSP del binario legacy. Consente Google Sign-In (GSI: script/style/connect/frame verso
     * accounts.google.com) e le immagini prodotto di OpenFoodFacts; blocca il resto (default-src 'self').
     */
    private static final String CSP_LEGACY =
        "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; "
      + "img-src 'self' data: https://images.openfoodfacts.org; font-src 'self'; "
      + "style-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/style; "
      + "script-src 'self' https://accounts.google.com/gsi/client; "
      + "connect-src 'self' https://accounts.google.com/gsi/ https://accounts.google.com/.well-known/; "
      + "frame-src 'self' blob: https://accounts.google.com/gsi/ https://accounts.google.com/o/oauth2/";

    /** Filtro che valida i JWT in ingresso. */
    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter();
    }

    /** Regole HTTP: /api/auth/** pubblico, il resto protetto, stateless. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/uploads/loghi/**").permitAll()
                // Difesa in profondità: gli endpoint admin richiedono SUPER_ADMIN già a livello di filter chain,
                // oltre a @PreAuthorize sul controller.
                .requestMatchers("/api/admin/**").hasAuthority("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        // B6 — header di sicurezza (CSP/HSTS/Referrer/Permissions-Policy/COOP), punto unico condiviso.
        SecurityHeaderSupport.applyBrowserHeaders(http, CSP_LEGACY, cspReportOnly);

        return http.build();
    }
}
