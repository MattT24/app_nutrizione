package it.nutrizionista.restnutrizionista.config;

import org.springframework.beans.factory.annotation.Qualifier;
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
                .requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources",
                        "/swagger-resources/**",
                        "/configuration/ui",
                        "/configuration/security",
                        "/webjars/**"
                    ).permitAll()
                // Difesa in profondità: gli endpoint admin richiedono SUPER_ADMIN già a livello di filter chain,
                // oltre a @PreAuthorize sul controller.
                .requestMatchers("/api/admin/**").hasAuthority("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
