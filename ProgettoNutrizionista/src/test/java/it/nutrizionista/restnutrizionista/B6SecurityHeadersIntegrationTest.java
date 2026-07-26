package it.nutrizionista.restnutrizionista;

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

/**
 * B6 — verifica che gli header di sicurezza siano presenti sul binario <b>legacy</b> (default).
 * Endpoint permitAll {@code /v3/api-docs}: l'{@code HeaderWriterFilter} scrive gli header a prescindere
 * dallo status della risposta. HSTS è scritto SOLO su richiesta HTTPS ({@code .secure(true)}).
 * Il binario keycloak è coperto da {@link B6SecurityHeadersKeycloakTest} (stesso file).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class B6SecurityHeadersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void legacy_browserHeaders_present() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                // binario legacy: la CSP consente Google Sign-In (GSI)
                .andExpect(header().string("Content-Security-Policy", containsString("accounts.google.com/gsi/client")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=(), payment=(), usb=()"))
                .andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin-allow-popups"));
    }

    @Test
    void legacy_hsts_soloSuHttps() throws Exception {
        mockMvc.perform(get("/v3/api-docs").secure(true))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }
}

/**
 * B6 — stessa verifica sul binario <b>keycloak</b> ({@code auth.provider=keycloak}). La CSP è quella
 * keycloak: SENZA GSI ({@code script-src 'self'}, niente {@code accounts.google.com}). I bean KC di test
 * (ClientRegistration + JwtDecoder mock, nessuna rete) sono riusati da
 * {@link KeycloakSecurityConfigMockMvcTest.KeycloakTestBeans}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.provider=keycloak")
@Import(KeycloakSecurityConfigMockMvcTest.KeycloakTestBeans.class)
class B6SecurityHeadersKeycloakTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void keycloak_browserHeaders_present_senzaGsi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("accounts.google.com"))))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin-allow-popups"));
    }

    @Test
    void keycloak_hsts_soloSuHttps() throws Exception {
        mockMvc.perform(get("/v3/api-docs").secure(true))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }
}
