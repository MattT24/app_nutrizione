package it.nutrizionista.restnutrizionista.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Fail-closed sul profilo (nota Code): nessun profilo primario → avvio fallisce; dev/prod/test (anche
 * combinati con keycloak) → ok. Unit test puro (niente context): ogni @SpringBootTest ha 'test' attivo,
 * quindi il ramo di fallimento va provato in isolamento con MockEnvironment.
 */
class ProfileStartupValidatorTest {

    private ProfileStartupValidator validatorWith(String... profili) {
        MockEnvironment env = new MockEnvironment();
        if (profili.length > 0) env.setActiveProfiles(profili);
        return new ProfileStartupValidator(env);
    }

    @Test
    void profiliPrimari_nonFalliscono() {
        assertThatCode(() -> validatorWith("dev").validate()).doesNotThrowAnyException();
        assertThatCode(() -> validatorWith("prod").validate()).doesNotThrowAnyException();
        assertThatCode(() -> validatorWith("test").validate()).doesNotThrowAnyException();
        assertThatCode(() -> validatorWith("dev", "keycloak").validate()).doesNotThrowAnyException();
        assertThatCode(() -> validatorWith("prod", "keycloak").validate()).doesNotThrowAnyException();
    }

    @Test
    void nessunProfilo_oSoloSecondario_falliscono() {
        assertThatThrownBy(() -> validatorWith().validate())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("profilo");
        assertThatThrownBy(() -> validatorWith("keycloak").validate())
                .isInstanceOf(IllegalStateException.class);
    }
}
