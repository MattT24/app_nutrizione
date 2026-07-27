package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

/**
 * B6 — verifica che gli header di sicurezza siano presenti sul binario <b>legacy</b> (default).
 * Ancora la richiesta a {@code /uploads/loghi/**} (permitAll in entrambi i binari, risorsa statica
 * inesistente → 404): l'{@code HeaderWriterFilter} scrive gli header al commit della risposta, quindi lo
 * status è indifferente, ma un path <b>permitAll</b> evita di dipendere dall'entry-point di autenticazione
 * (nel binario keycloak una risorsa protetta risponderebbe con il redirect di {@code oauth2Login}).
 * ⚠️ Non usare {@code /v3/api-docs}: springdoc è stato rimosso dal pom, quel path non esiste più.
 * Il binario keycloak è coperto da {@link B6SecurityHeadersKeycloakTest} (stesso file).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class B6SecurityHeadersIntegrationTest {

    /** Path permitAll su entrambe le chain: ancora stabile per leggere gli header di risposta. */
    private static final String PROBE = "/uploads/loghi/probe-header.png";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void legacy_browserHeaders_present() throws Exception {
        mockMvc.perform(get(PROBE))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                // binario legacy: la CSP consente Google Sign-In (GSI)
                .andExpect(header().string("Content-Security-Policy", containsString("accounts.google.com/gsi/client")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=(), payment=(), usb=()"))
                .andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin-allow-popups"));
    }

    /**
     * nosniff e X-Frame-Options sono default di Spring Security, non scritti da
     * {@code SecurityHeaderSupport}: asserirli qui li rende regressione visibile se un domani una chain
     * personalizzasse {@code .headers()} disattivandoli.
     */
    @Test
    void legacy_defaultHardeningHeaders_present() throws Exception {
        mockMvc.perform(get(PROBE))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void legacy_hsts_soloSuHttps() throws Exception {
        mockMvc.perform(get(PROBE).secure(true))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }
}

/**
 * B6 — stessa verifica sul binario <b>keycloak</b> ({@code auth.provider=keycloak}). La CSP è quella
 * keycloak: SENZA GSI ({@code script-src 'self'}, niente {@code accounts.google.com}). I bean KC di test
 * (ClientRegistration + JwtDecoder mock, nessuna rete) sono riusati da
 * {@link KeycloakSecurityConfigMockMvcTest.KeycloakTestBeans}.
 *
 * <p>{@code app.security.cookie-secure=true} riproduce la configurazione di prod
 * ({@code application-prod.properties:48}) per verificare i flag del cookie XSRF. Il cookie di SESSIONE
 * ({@code server.servlet.session.cookie.*}) è invece configurazione del servlet container, che MockMvc non
 * istanzia: quel contratto è coperto da {@link ProdConfigContractTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = { "auth.provider=keycloak", "app.security.cookie-secure=true" })
@Import(KeycloakSecurityConfigMockMvcTest.KeycloakTestBeans.class)
class B6SecurityHeadersKeycloakTest {

    private static final String PROBE = "/uploads/loghi/probe-header.png";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void keycloak_browserHeaders_present_senzaGsi() throws Exception {
        mockMvc.perform(get(PROBE))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("accounts.google.com"))))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin-allow-popups"));
    }

    @Test
    void keycloak_defaultHardeningHeaders_present() throws Exception {
        mockMvc.perform(get(PROBE))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void keycloak_hsts_soloSuHttps() throws Exception {
        mockMvc.perform(get(PROBE).secure(true))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }

    /**
     * Il cookie XSRF-TOKEN deve uscire con {@code Secure} + {@code SameSite=Lax} quando
     * {@code app.security.cookie-secure=true} (prod); {@code HttpOnly} resta assente per design (la SPA
     * Angular deve rileggere il token per il double-submit).
     *
     * <p>⚠️ Verificato sull'<b>oggetto</b> {@code Cookie}, non sull'header {@code Set-Cookie}: con un
     * {@code sameSite} valorizzato, {@code CookieCsrfTokenRepository} (6.5.x) emette una
     * {@code jakarta.servlet.http.Cookie} con {@code setAttribute("SameSite", …)} via {@code addCookie()};
     * {@code MockHttpServletResponse} serializza {@code SameSite} nell'header solo per i {@code MockCookie}
     * (verificato: {@code instanceof MockCookie} nel suo {@code addCookie}), quindi nel mock l'attributo non
     * comparirebbe nell'header pur essendo impostato. A runtime è il container a serializzarlo → l'header
     * reale va comunque ispezionato in verifica live.
     */
    @Test
    void keycloak_cookieXsrf_secureESameSiteLax() throws Exception {
        MvcResult result = mockMvc.perform(get(PROBE)).andReturn();

        Cookie xsrf = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).as("cookie XSRF-TOKEN materializzato dal CsrfCookieFilter").isNotNull();
        assertThat(xsrf.getSecure()).as("flag Secure con cookie-secure=true").isTrue();
        assertThat(xsrf.getAttribute("SameSite")).as("SameSite allineato al design BFF").isEqualTo("Lax");
        assertThat(xsrf.isHttpOnly()).as("leggibile da JS per il double-submit Angular").isFalse();
    }
}
