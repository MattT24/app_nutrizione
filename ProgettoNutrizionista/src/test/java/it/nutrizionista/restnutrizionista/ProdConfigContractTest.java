package it.nutrizionista.restnutrizionista;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * B6 — guard di regressione sulle chiavi di sicurezza del profilo <b>prod</b>.
 *
 * <p>Perché un contract-test sul file e non un test di runtime: {@code server.forward-headers-strategy} e
 * {@code server.servlet.session.cookie.*} sono configurazione del <b>servlet container</b>, che MockMvc non
 * istanzia — nessun test di integrazione le esercita, quindi una loro rimozione passerebbe con la suite verde
 * e si manifesterebbe solo in produzione (cookie di sessione senza {@code Secure}, IP di audit = quello del
 * proxy). Qui si verifica il contratto dove è verificabile: il file committato.
 * Gli header e il cookie XSRF, che invece passano dalla filter chain, sono coperti a runtime da
 * {@code B6SecurityHeadersIntegrationTest}.
 */
class ProdConfigContractTest {

    private static final Properties PROD = new Properties();

    @BeforeAll
    static void caricaProfiloProd() throws Exception {
        try (InputStream in = ProdConfigContractTest.class.getResourceAsStream("/application-prod.properties")) {
            assertThat(in).as("application-prod.properties presente sul classpath").isNotNull();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                PROD.load(reader);
            }
        }
    }

    /**
     * Dietro il reverse-proxy che termina il TLS, senza questa strategia Spring vede HTTP: HSTS non viene
     * scritto, il flag {@code Secure} sui cookie decade e {@code getRemoteAddr()} restituisce l'IP del proxy
     * invece di quello del client (audit A7 falsato).
     */
    @Test
    void prod_onoraGliHeaderDelProxy() {
        assertThat(PROD.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
    }

    /**
     * {@code same-site=lax} è deliberato, NON {@code strict}: con strict il cookie non verrebbe rimandato sul
     * redirect di ritorno dell'IdP → login OIDC rotto in prod.
     */
    @Test
    void prod_cookieDiSessioneSecureELax() {
        assertThat(PROD.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
        assertThat(PROD.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
    }

    /** Cookie XSRF con Secure e CSP in enforce (non report-only) sono lo stato atteso a regime in prod. */
    @Test
    void prod_cookieCsrfSecureECspInEnforce() {
        assertThat(PROD.getProperty("app.security.cookie-secure")).isEqualTo("true");
        assertThat(PROD.getProperty("app.security.csp.report-only")).isEqualTo("false");
    }

    /**
     * In prod il super admin non ha default: {@code SuperAdminProductionSecurityValidator} fallisce l'avvio se
     * le variabili mancano. Il guard protegge il fatto che restino <b>senza fallback committato</b>.
     */
    @Test
    void prod_superAdminSenzaDefaultCommittati() {
        assertThat(PROD.getProperty("superadmin.email")).isEqualTo("${SUPERADMIN_EMAIL:}");
        assertThat(PROD.getProperty("superadmin.password-hash")).isEqualTo("${SUPERADMIN_PASSWORD_HASH:}");
    }
}
