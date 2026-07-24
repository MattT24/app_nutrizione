package it.nutrizionista.restnutrizionista.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import it.nutrizionista.restnutrizionista.security.UserDetailsServiceImpl;

/**
 * Bean di sicurezza <b>indipendenti dal binario</b> (legacy-jwt / keycloak): sono sempre caricati, così
 * {@code AuthService}/{@code AuthController} e il method-security funzionano in entrambi i mode.
 *
 * <p>La <b>SecurityFilterChain</b> vive invece nelle config gated per-binario ({@code LegacySecurityConfig} /
 * {@code KeycloakSecurityConfig}). {@code @EnableMethodSecurity} è qui (sempre attivo) → {@code @PreAuthorize} vale
 * in entrambi i binari.
 */
@Configuration
@EnableMethodSecurity
public class SecurityBeansConfig {

    /** Origini CORS consentite (lista separata da virgole), da application-{profile}.properties / env. */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * CORS centralizzato e parametrizzato. Origini da property: dev = http://localhost:4200; prod = da
     * APP_CORS_ALLOWED_ORIGINS. In legacy il Bearer è in header → allowCredentials=false; in keycloak-mode dev
     * si usa il dev-proxy same-origin (CORS ininfluente), quindi si mantiene false anche qui.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(o -> !o.isEmpty()).toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Encoder per password sicure (login legacy + register). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Espone l'AuthenticationManager per l'AuthController (login legacy). */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsServiceImpl userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
