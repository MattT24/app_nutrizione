package it.nutrizionista.restnutrizionista.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.exception.TooManyRequestsException;

/** Rate limit separato per login demo e chiave di impersonazione. */
@Service
public class DemoRateLimitService {
    private static final int MAX_LOGIN = 5;
    private static final int MAX_IMPERSONAZIONE = 5;
    private static final Duration FINESTRA = Duration.ofMinutes(15);
    private record Tentativi(int conteggio, Instant ultimo) {}
    private final Map<String, Tentativi> loginFailures = new ConcurrentHashMap<>();
    private final Map<String, Tentativi> impersonationFailures = new ConcurrentHashMap<>();
    @Autowired private Clock clock;

    public void verificaLogin(String username, String ip) {
        verifica(loginFailures, chiave(username, ip), MAX_LOGIN);
    }
    public void fallimentoLogin(String username, String ip) {
        registra(loginFailures, chiave(username, ip));
    }
    public void successoLogin(String username, String ip) {
        loginFailures.remove(chiave(username, ip));
    }
    public void verificaImpersonazione(Long adminId, String ip) {
        verifica(impersonationFailures, chiave(String.valueOf(adminId), ip), MAX_IMPERSONAZIONE);
    }
    public void fallimentoImpersonazione(Long adminId, String ip) {
        registra(impersonationFailures, chiave(String.valueOf(adminId), ip));
    }
    public void successoImpersonazione(Long adminId, String ip) {
        impersonationFailures.remove(chiave(String.valueOf(adminId), ip));
    }

    private void verifica(Map<String, Tentativi> mappa, String key, int massimo) {
        Tentativi t = mappa.get(key);
        if (t == null) return;
        if (t.ultimo().plus(FINESTRA).isBefore(clock.instant())) {
            mappa.remove(key);
        } else if (t.conteggio() >= massimo) {
            throw new TooManyRequestsException("Troppi tentativi. Riprova tra qualche minuto.");
        }
    }
    private void registra(Map<String, Tentativi> mappa, String key) {
        Instant now = clock.instant();
        mappa.compute(key, (k, old) -> old == null || old.ultimo().plus(FINESTRA).isBefore(now)
                ? new Tentativi(1, now) : new Tentativi(old.conteggio() + 1, now));
    }
    private String chiave(String identita, String ip) {
        return (identita == null ? "" : identita.trim().toLowerCase()) + "|" + (ip == null ? "" : ip);
    }
    @Scheduled(fixedDelayString = "${demo.rate-limit.cleanup-ms:600000}")
    public void pulisci() {
        Instant limite = clock.instant().minus(FINESTRA);
        loginFailures.entrySet().removeIf(e -> e.getValue().ultimo().isBefore(limite));
        impersonationFailures.entrySet().removeIf(e -> e.getValue().ultimo().isBefore(limite));
    }
}
