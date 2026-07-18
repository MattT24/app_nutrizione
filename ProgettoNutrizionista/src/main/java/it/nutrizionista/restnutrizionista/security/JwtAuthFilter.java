package it.nutrizionista.restnutrizionista.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * __Filtro__ per __validare__ JWT e __impostare__ SecurityContext.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired private JwtUtils jwtUtils;
    @Autowired private DemoTokenValidationService demoTokenValidationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = parseJwt(request);
        try {
            if (jwt != null) {
                if (!jwtUtils.validateJwtToken(jwt)) {
                    if (!isAuthEndpoint(request)) {
                        scriviUnauthorized(response, "TOKEN_NON_VALIDO");
                        return;
                    }
                } else {
                    Claims claims = Jwts.parser().verifyWith(jwtUtils.getKey()).build()
                            .parseSignedClaims(jwt).getPayload();
                    if ("DEMO".equals(claims.get("accountType", String.class))) {
                        DemoTokenValidationService.Risultato risultato = demoTokenValidationService.valida(claims);
                        if (!risultato.valido()) {
                            scriviUnauthorized(response, risultato.codice());
                            return;
                        }
                    }
                    @SuppressWarnings("unchecked")
                    List<String> authorities = claims.get("authorities", List.class);
                    List<GrantedAuthority> grantedAuthorities = authorities != null
                            ? authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                            : List.of();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, grantedAuthorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            if (jwt != null && !isAuthEndpoint(request)) {
                logger.warn("JWT non accettato per una richiesta protetta");
                scriviUnauthorized(response, "TOKEN_NON_VALIDO");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth/");
    }

    private void scriviUnauthorized(HttpServletResponse response, String codice) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + codice + "\",\"message\":\"Sessione non valida o scaduta\"}");
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}