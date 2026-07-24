package it.nutrizionista.restnutrizionista.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

/**
 * {@code getMe()} risolve per {@code subjectId} (binario keycloak, principal-name = sub) con fallback email
 * (binario legacy, principal-name = email). ⚠️ Guard I2: in keycloak-mode il fallback-email NON scatta
 * (subjectId presente) → nessuna risoluzione-tenant-per-email accidentale.
 */
class CurrentUserServiceGetMeTest {

    private final UtenteRepository repo = mock(UtenteRepository.class);
    private final CurrentUserService svc = new CurrentUserService();

    CurrentUserServiceGetMeTest() {
        ReflectionTestUtils.setField(svc, "repoUtente", repo);
    }

    private void authenticateAs(String principalName) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principalName, null));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keycloakMode_resolvesBySubject_noEmailFallback() {
        Utente u = new Utente();
        authenticateAs("sub-uuid-123");
        when(repo.findWithAuthoritiesBySubjectId("sub-uuid-123")).thenReturn(Optional.of(u));

        assertThat(svc.getMe()).isSameAs(u);
        // il fallback-email non deve scattare quando il subjectId matcha
        verify(repo, never()).findWithAuthoritiesByEmail(any());
    }

    @Test
    void legacyMode_fallsBackToEmail() {
        Utente u = new Utente();
        authenticateAs("nutrizionista@studio.it");
        when(repo.findWithAuthoritiesBySubjectId("nutrizionista@studio.it")).thenReturn(Optional.empty());
        when(repo.findWithAuthoritiesByEmail("nutrizionista@studio.it")).thenReturn(Optional.of(u));

        assertThat(svc.getMe()).isSameAs(u);
    }

    @Test
    void notFound_throws() {
        authenticateAs("sub-ignoto");
        when(repo.findWithAuthoritiesBySubjectId("sub-ignoto")).thenReturn(Optional.empty());
        when(repo.findWithAuthoritiesByEmail("sub-ignoto")).thenReturn(Optional.empty());

        assertThatThrownBy(svc::getMe).isInstanceOf(NotFoundException.class);
    }
}
