package it.nutrizionista.restnutrizionista.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * B6 — header di sicurezza HTTP condivisi tra i binari (legacy / keycloak). Punto UNICO.
 * nosniff + X-Frame-Options DENY + HSTS restano i default di Spring Security; qui si aggiunge il resto
 * e si rende HSTS esplicito (preload/maxAge). CSP/HSTS/Referrer via DSL (stabili); Permissions-Policy + COOP
 * via StaticHeadersWriter = version-proof (evita "metodo DSL assente su 6.5", come accadde con .csrf().spa()).
 */
final class SecurityHeaderSupport {

    private SecurityHeaderSupport() {}

    private static final String PERMISSIONS_POLICY =
            "camera=(), microphone=(), geolocation=(), payment=(), usb=()";

    /** Chain BROWSER (serve/può servire HTML). csp = policy per-binario; reportOnly da property. */
    static void applyBrowserHeaders(HttpSecurity http, String csp, boolean reportOnly) throws Exception {
        http.headers(h -> {
            h.contentSecurityPolicy(c -> {
                c.policyDirectives(csp);
                if (reportOnly) c.reportOnly();
            });
            h.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .preload(false)                 // opt-in dopo conferma apex/subdomain
                    .maxAgeInSeconds(31_536_000));   // 1 anno
            h.referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
            // same-origin-allow-popups: isolamento XS-leak SENZA rompere i popup OAuth/GSI.
            h.addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin-allow-popups"));
            h.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY));
        });
    }

    /** Chain API/bearer (/api-mobile/**, solo JSON): CSP minimale + HSTS + no-referrer. */
    static void applyApiHeaders(HttpSecurity http) throws Exception {
        http.headers(h -> {
            h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
            h.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000));
            h.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER));
        });
    }
}
