package it.nutrizionista.restnutrizionista.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.exception.TrattamentoLimitatoException;

/**
 * Unit test del {@link LimitazioneTrattamentoValidator} (A5.3): il check è puro, senza Spring.
 */
class LimitazioneTrattamentoValidatorTest {

    private final LimitazioneTrattamentoValidator validator = new LimitazioneTrattamentoValidator();

    @Test
    void clienteLimitato_lancia423() {
        Cliente c = new Cliente();
        c.setTrattamentoLimitato(true);
        assertThatThrownBy(() -> validator.assertNonLimitato(c))
                .isInstanceOf(TrattamentoLimitatoException.class);
    }

    @Test
    void clienteNonLimitato_nonLancia() {
        Cliente c = new Cliente();
        c.setTrattamentoLimitato(false);
        assertThatCode(() -> validator.assertNonLimitato(c)).doesNotThrowAnyException();
    }

    @Test
    void clienteNull_nonLancia() {
        // Difesa: appuntamenti senza cliente registrato → no-op (la validazione a monte gestisce il null).
        assertThatCode(() -> validator.assertNonLimitato(null)).doesNotThrowAnyException();
    }

    @Test
    void defaultCliente_nonELimitato() {
        // Il flag nasce false (nessun cliente pre-limitato alla creazione).
        assertThat(new Cliente().isTrattamentoLimitato()).isFalse();
    }
}
