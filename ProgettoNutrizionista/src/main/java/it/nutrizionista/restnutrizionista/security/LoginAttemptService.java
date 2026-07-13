package it.nutrizionista.restnutrizionista.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protezione anti brute-force sul login: traccia i tentativi falliti per coppia
 * email+IP e blocca temporaneamente dopo troppi errori consecutivi.
 * In-memory: sufficiente per singola istanza; con più istanze servirebbe uno store condiviso.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    @Value("${security.login.max-tentativi:5}")
    private int maxTentativi;

    @Value("${security.login.blocco-minuti:15}")
    private long bloccoMinuti;

    private record Attempts(int count, Instant lastFailure) {}

    private final Map<String, Attempts> failures = new ConcurrentHashMap<>();

    private String key(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase()) + "|" + (ip == null ? "" : ip);
    }

    /** True se la coppia email+IP è attualmente bloccata. */
    public boolean isBlocked(String email, String ip) {
        Attempts a = failures.get(key(email, ip));
        if (a == null) return false;
        if (a.count() < maxTentativi) return false;
        if (a.lastFailure().plus(Duration.ofMinutes(bloccoMinuti)).isBefore(Instant.now())) {
            failures.remove(key(email, ip));
            return false;
        }
        return true;
    }

    /** Registra un tentativo fallito. */
    public void registraFallimento(String email, String ip) {
        failures.merge(key(email, ip), new Attempts(1, Instant.now()),
                (old, x) -> new Attempts(old.count() + 1, Instant.now()));
        Attempts a = failures.get(key(email, ip));
        if (a != null && a.count() >= maxTentativi) {
            // Non logghiamo mai la password; email troncata per non riempire i log di dati personali.
            log.warn("Login bloccato per troppi tentativi falliti (ip={}, tentativi={})", ip, a.count());
        }
    }

    /** Azzera i contatori dopo un login riuscito. */
    public void registraSuccesso(String email, String ip) {
        failures.remove(key(email, ip));
    }

    /** Pulizia periodica delle voci scadute per evitare crescita illimitata della mappa. */
    @Scheduled(fixedDelayString = "${security.login.pulizia-ms:600000}")
    public void pulisciScaduti() {
        Instant limite = Instant.now().minus(Duration.ofMinutes(bloccoMinuti));
        failures.entrySet().removeIf(e -> e.getValue().lastFailure().isBefore(limite));
    }
}
