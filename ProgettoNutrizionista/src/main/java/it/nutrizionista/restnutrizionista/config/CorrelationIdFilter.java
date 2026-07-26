package it.nutrizionista.restnutrizionista.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A1 osservabilità — correlation-id end-to-end. Legge l'header {@code X-Correlation-Id} (se il client lo invia, es.
 * il FE) oppure ne genera uno; lo mette in MDC (chiave {@code correlationId}, usata dal pattern di log) e lo riespone
 * come header di response così il SPA può correlare richiesta ↔ log.
 *
 * <p>NON è {@code @Component}: è registrato SOLO via {@code FilterRegistrationBean} in {@link ObservabilityConfig} a
 * {@code HIGHEST_PRECEDENCE}, FUORI dalla FilterChainProxy di Spring Security → vale per il binario legacy e per
 * entrambe le chain keycloak senza toccare gli {@code addFilterBefore/After} esistenti. (Se fosse {@code @Component}
 * Boot lo auto-registrerebbe anche nella catena servlet → doppia registrazione.)
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
