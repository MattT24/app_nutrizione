package it.nutrizionista.restnutrizionista.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * A1 osservabilità — registra il {@link CorrelationIdFilter} a {@link Ordered#HIGHEST_PRECEDENCE} nel servlet
 * container, PRIMA dell'intera FilterChainProxy di Spring Security → il correlation-id è in MDC per TUTTA la richiesta
 * (binario legacy + entrambe le chain keycloak) senza toccare gli {@code addFilterBefore/After} esistenti. Il filtro
 * è istanziato e registrato SOLO qui (non è {@code @Component}) per evitare la doppia registrazione automatica di Boot.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setName("correlationIdFilter");
        return registration;
    }
}
