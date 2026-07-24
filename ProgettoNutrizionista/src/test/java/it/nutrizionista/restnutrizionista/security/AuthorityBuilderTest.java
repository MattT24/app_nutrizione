package it.nutrizionista.restnutrizionista.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import it.nutrizionista.restnutrizionista.entity.Permesso;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.RuoloPermesso;
import it.nutrizionista.restnutrizionista.entity.Utente;

/**
 * Le authorities sono gli alias dei permessi (stringhe nude), inclusa {@code SUPER_ADMIN} (permesso oltre che
 * ruolo) → il gate {@code hasAuthority('SUPER_ADMIN')} su /api/admin/** resta valido in tutti i binari.
 */
class AuthorityBuilderTest {

    private final AuthorityBuilder builder = new AuthorityBuilder();

    @Test
    void build_returnsPermissionAliases_includingSuperAdmin() {
        Utente u = mock(Utente.class);
        Ruolo r = mock(Ruolo.class);
        RuoloPermesso rp1 = mock(RuoloPermesso.class);
        RuoloPermesso rp2 = mock(RuoloPermesso.class);
        Permesso p1 = mock(Permesso.class);
        Permesso p2 = mock(Permesso.class);
        when(u.getRuolo()).thenReturn(r);
        when(r.getRuoloPermessi()).thenReturn(List.of(rp1, rp2));
        when(rp1.getPermesso()).thenReturn(p1);
        when(rp2.getPermesso()).thenReturn(p2);
        when(p1.getAlias()).thenReturn("ALIMENTO_READ");
        when(p2.getAlias()).thenReturn("SUPER_ADMIN");

        assertThat(builder.build(u))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ALIMENTO_READ", "SUPER_ADMIN");
    }

    @Test
    void build_isNullSafe() {
        assertThat(builder.build(null)).isEmpty();

        Utente noRuolo = mock(Utente.class);
        when(noRuolo.getRuolo()).thenReturn(null);
        assertThat(builder.build(noRuolo)).isEmpty();
    }
}
