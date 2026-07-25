package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

@Service
public class CurrentUserService {

    @Autowired
    private UtenteRepository repoUtente;

    public Utente getMe() {
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();

        // Binario keycloak: principal-name = `sub` (UUID stabile) → risolvi per subjectId.
        // Binario legacy:   principal-name = email → fallback per email.
        // ⚠️ Guard (I2): in keycloak-mode il name è il `sub`, quindi findWithAuthoritiesByEmail(sub) NON matcha mai
        // (nessuna risoluzione-tenant-per-email accidentale su dati art. 9); post-link il subjectId è sempre presente
        // → il fallback non scatta. Il JOIN FETCH prende Utente + Ruolo (+ Permessi) in un colpo solo.
        return repoUtente.findWithAuthoritiesBySubjectId(principalName)
                .or(() -> repoUtente.findWithAuthoritiesByEmail(principalName))
                .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));
    }
}
