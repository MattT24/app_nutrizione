package it.nutrizionista.restnutrizionista.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import it.nutrizionista.restnutrizionista.security.JwtAuthFilter;
import it.nutrizionista.restnutrizionista.security.UserDetailsServiceImpl;

/**
 * Configurazione Spring Security con JWT stateless e @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /** Origini CORS consentite (lista separata da virgole), da application-{profile}.properties / env. */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * CORS centralizzato e parametrizzato (sostituisce i @CrossOrigin hardcoded sui controller).
     * Origini da property: dev = http://localhost:4200; prod = da APP_CORS_ALLOWED_ORIGINS.
     * Bearer token in header (non cookie) → allowCredentials=false.
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

    /** Filtro che valida i JWT in ingresso. */
    @Bean
    public JwtAuthFilter jwtAuthFilter() { return new JwtAuthFilter(); }

    /** Encoder per password sicure. */
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    /** Espone l'AuthenticationManager per l'AuthController. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
    
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    /** Regole HTTP: /api/auth/** pubblico, il resto protetto, stateless. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                // Difesa in profondità: gli endpoint admin richiedono SUPER_ADMIN
                // già a livello di filter chain, oltre a @PreAuthorize sul controller.
                .requestMatchers("/api/admin/**").hasAuthority("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            //.authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
