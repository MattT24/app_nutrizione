package it.nutrizionista.restnutrizionista.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Fail-fast sulla selezione del binario (nota Code): valore ignoto → avvio fallisce (fail-closed),
 * valori noti dell'enum → ok. La validità del MODE (1 chain legacy / 2 keycloak) è coperta funzionalmente
 * dal suite default (legacy) + dal test keycloak-mode.
 */
class AuthProviderStartupValidatorTest {

    private AuthProviderStartupValidator validatorWith(String value) {
        AuthProviderStartupValidator v = new AuthProviderStartupValidator();
        ReflectionTestUtils.setField(v, "authProvider", value);
        return v;
    }

    @Test
    void knownValues_doNotThrow() {
        assertThatCode(() -> validatorWith("legacy-jwt").validate()).doesNotThrowAnyException();
        assertThatCode(() -> validatorWith("keycloak").validate()).doesNotThrowAnyException();
    }

    @Test
    void unknownValue_failsStartup() {
        assertThatThrownBy(() -> validatorWith("keycloack-typo").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.provider");
        assertThatThrownBy(() -> validatorWith("legacy-jwt ").validate())
                .isInstanceOf(IllegalStateException.class);
    }
}
