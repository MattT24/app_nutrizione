package it.nutrizionista.restnutrizionista.security;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import it.nutrizionista.restnutrizionista.entity.CredenzialeDemo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.CredenzialeDemoRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

/** Convalida server-side revoca, scadenza e attore dei JWT demo/impersonati. */
@Service
public class DemoTokenValidationService {
    public record Risultato(boolean valido, String codice) {
        public static Risultato ok() { return new Risultato(true, null); }
        public static Risultato errore(String codice) { return new Risultato(false, codice); }
    }
    @Autowired private CredenzialeDemoRepository demoRepository;
    @Autowired private UtenteRepository utenteRepository;
    @Autowired private Clock clock;

    @Transactional(readOnly = true)
    public Risultato valida(Claims claims) {
        Long id = numero(claims.get("demoCredentialId"));
        Long versione = numero(claims.get("tokenVersion"));
        if (id == null || versione == null) return Risultato.errore("TOKEN_DEMO_INVALIDO");
        CredenzialeDemo demo = demoRepository.findConUtenteById(id).orElse(null);
        if (demo == null || demo.getTokenVersion() != versione.longValue()
                || !demo.getUtente().getEmail().equals(claims.getSubject())) {
            return Risultato.errore("SESSIONE_REVOCATA");
        }
        boolean impersonazione = Boolean.TRUE.equals(claims.get("impersonation", Boolean.class));
        if (impersonazione) {
            Long adminId = numero(claims.get("actorAdminId"));
            Utente admin = adminId == null ? null : utenteRepository.findById(adminId).orElse(null);
            if (admin == null || admin.getRuolo() == null
                    || !"SUPER_ADMIN".equals(admin.getRuolo().getAlias())) {
                return Risultato.errore("IMPERSONAZIONE_REVOCATA");
            }
            return Risultato.ok();
        }
        if (demo.getDisabledAt() != null) return Risultato.errore("DEMO_DISABILITATO");
        if (!demo.getExpiresAt().isAfter(clock.instant())) return Risultato.errore("DEMO_SCADUTO");
        return Risultato.ok();
    }

    private Long numero(Object valore) {
        return valore instanceof Number n ? n.longValue() : null;
    }
}
