package it.nutrizionista.restnutrizionista.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A2 osservabilità — chain di sicurezza DEDICATA agli endpoint Actuator, attiva <b>solo in profilo {@code dev}</b>.
 * {@code securityMatcher("/actuator/**")} + {@code @Order(HIGHEST_PRECEDENCE)} (ordine unico: legacy usa LOWEST,
 * keycloak 0/1) → in dev gli endpoint sono raggiungibili per diagnostica su localhost.
 *
 * <p>In <b>PROD</b> questa config NON è caricata → {@code /actuator/**} ricade sulla chain principale
 * ({@code anyRequest().authenticated()}) e {@code management.endpoints.web.exposure} resta al default (solo health).
 * CSRF off: endpoint diagnostici locali, nessun form/cookie. Il {@link CorrelationIdFilter} (HIGHEST_PRECEDENCE nel
 * servlet container) resta comunque a monte anche per /actuator.
 */
@Configuration
@Profile("dev")
public class ActuatorDevSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain actuatorDevSecurityChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
