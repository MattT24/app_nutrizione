package it.nutrizionista.restnutrizionista.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

/**
 * Link identità Keycloak ({@code sub}) ↔ {@code Utente} DB al primo login OIDC (criterio I2 del design).
 *
 * <p>Regole vincolanti (verifica Code):
 * <ol>
 *   <li>se esiste già un {@code Utente} con quel {@code subjectId} → usalo (idempotente);</li>
 *   <li>altrimenti, <b>solo se {@code email_verified==true}</b> e un {@code Utente} matcha l'email con
 *       {@code subjectId} null → <b>timbra</b> {@code subjectId} (lo unique constraint cattura la race) → usalo.
 *       Il gate su {@code email_verified} preserva il controllo già presente in {@code GoogleTokenVerifier}
 *       (altrimenti sarebbe una regressione → account-takeover su dati art. 9);</li>
 *   <li>altrimenti <b>REJECT</b> — <b>niente auto-provisioning silenzioso</b> (creerebbe un account bypassando
 *       la cattura accettazioni/base-giuridica art. 6(1)(b) di {@code AuthService.register}).</li>
 * </ol>
 * L'{@code Utente} restituito ha ruolo+permessi già inizializzati (join fetch) → l'{@code AuthorityBuilder}
 * può costruire le authorities dalla DB.
 */
@Service
public class KeycloakUserLinkService {

    private final UtenteRepository utenteRepository;

    public KeycloakUserLinkService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    @Transactional
    public Utente linkOrReject(String sub, String email, boolean emailVerified) {
        if (!StringUtils.hasText(sub)) {
            throw reject("invalid_subject", "Token OIDC senza subject");
        }
        // 1. già linkato
        var bySub = utenteRepository.findWithAuthoritiesBySubjectId(sub);
        if (bySub.isPresent()) {
            return bySub.get();
        }
        // 2. link per email SOLO se verificata
        if (!emailVerified || !StringUtils.hasText(email)) {
            throw reject("email_not_verified", "Email non verificata o assente: link identità negato");
        }
        Utente byEmail = utenteRepository.findWithAuthoritiesByEmail(email).orElse(null);
        if (byEmail == null) {
            // niente auto-provisioning: l'onboarding di un nutrizionista resta un atto esplicito (accettazioni/base giuridica)
            throw reject("user_not_provisioned", "Nessun account Statera per questa identità: onboarding richiesto");
        }
        if (StringUtils.hasText(byEmail.getSubjectId())) {
            // l'account è già collegato a un ALTRO sub → conflitto (non ri-collegare)
            throw reject("subject_mismatch", "Account già collegato a un'altra identità");
        }
        // timbro idempotente; lo unique constraint su subject_id blinda la race di due primi-login concorrenti
        byEmail.setSubjectId(sub);
        utenteRepository.save(byEmail);
        return byEmail;
    }

    private OAuth2AuthenticationException reject(String code, String desc) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, desc, null), desc);
    }
}
