package it.nutrizionista.restnutrizionista.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

/**
 * Link identità Keycloak (`sub`) ↔ Utente al primo login (criterio I2): gate {@code email_verified},
 * niente auto-provisioning, timbro idempotente, no re-link su conflitto.
 */
class KeycloakUserLinkServiceTest {

    private final UtenteRepository repo = mock(UtenteRepository.class);
    private final KeycloakUserLinkService svc = new KeycloakUserLinkService(repo);

    @Test
    void alreadyLinked_returnsWithoutSaving() {
        Utente u = new Utente();
        u.setSubjectId("sub-1");
        when(repo.findWithAuthoritiesBySubjectId("sub-1")).thenReturn(Optional.of(u));

        assertThat(svc.linkOrReject("sub-1", "x@y.it", true)).isSameAs(u);
        verify(repo, never()).save(any());
    }

    @Test
    void emailNotVerified_rejects() {
        when(repo.findWithAuthoritiesBySubjectId("sub-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.linkOrReject("sub-2", "x@y.it", false))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void unknownEmail_rejects_noAutoProvisioning() {
        when(repo.findWithAuthoritiesBySubjectId("sub-3")).thenReturn(Optional.empty());
        when(repo.findWithAuthoritiesByEmail("x@y.it")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.linkOrReject("sub-3", "x@y.it", true))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void verifiedEmailMatch_stampsSubjectId() {
        Utente u = new Utente();
        u.setEmail("x@y.it");
        when(repo.findWithAuthoritiesBySubjectId("sub-4")).thenReturn(Optional.empty());
        when(repo.findWithAuthoritiesByEmail("x@y.it")).thenReturn(Optional.of(u));

        Utente res = svc.linkOrReject("sub-4", "x@y.it", true);

        assertThat(res.getSubjectId()).isEqualTo("sub-4");
        verify(repo).save(u);
    }

    @Test
    void alreadyLinkedToOtherSub_rejects() {
        Utente u = new Utente();
        u.setEmail("x@y.it");
        u.setSubjectId("other-sub");
        when(repo.findWithAuthoritiesBySubjectId("sub-5")).thenReturn(Optional.empty());
        when(repo.findWithAuthoritiesByEmail("x@y.it")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> svc.linkOrReject("sub-5", "x@y.it", true))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void blankSubject_rejects() {
        assertThatThrownBy(() -> svc.linkOrReject("  ", "x@y.it", true))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
