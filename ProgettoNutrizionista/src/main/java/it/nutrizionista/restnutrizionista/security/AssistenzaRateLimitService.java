package it.nutrizionista.restnutrizionista.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.exception.TooManyRequestsException;

/** Rate limiter in-memory per ticket e messaggi della feature assistenza. */
@Service
public class AssistenzaRateLimitService {
    private static final Duration FINESTRA_TICKET = Duration.ofHours(24);
    private static final Duration FINESTRA_MESSAGGI = Duration.ofMinutes(1);
    private final Map<Long, Deque<Instant>> ticketCreati = new ConcurrentHashMap<>();
    private final Map<Long, Deque<Instant>> messaggiInviati = new ConcurrentHashMap<>();

    public void verificaTicket(Long utenteId) {
        verifica(ticketCreati, utenteId, FINESTRA_TICKET, 3,
                "Limite di 3 richieste di assistenza ogni 24 ore superato");
    }
    public void registraTicket(Long utenteId) { registra(ticketCreati, utenteId, FINESTRA_TICKET); }
    public void verificaMessaggio(Long utenteId) {
        verifica(messaggiInviati, utenteId, FINESTRA_MESSAGGI, 20,
                "Troppi messaggi: attendi un minuto prima di riprovare");
    }
    public void registraMessaggio(Long utenteId) { registra(messaggiInviati, utenteId, FINESTRA_MESSAGGI); }

    private void verifica(Map<Long, Deque<Instant>> mappa, Long utenteId, Duration finestra,
            int massimo, String messaggio) {
        Deque<Instant> eventi = mappa.computeIfAbsent(utenteId, key -> new ArrayDeque<>());
        synchronized (eventi) {
            rimuoviScaduti(eventi, finestra);
            if (eventi.size() >= massimo) throw new TooManyRequestsException(messaggio);
        }
    }
    private void registra(Map<Long, Deque<Instant>> mappa, Long utenteId, Duration finestra) {
        Deque<Instant> eventi = mappa.computeIfAbsent(utenteId, key -> new ArrayDeque<>());
        synchronized (eventi) {
            rimuoviScaduti(eventi, finestra);
            eventi.addLast(Instant.now());
        }
    }
    private void rimuoviScaduti(Deque<Instant> eventi, Duration finestra) {
        Instant limite = Instant.now().minus(finestra);
        while (!eventi.isEmpty() && eventi.peekFirst().isBefore(limite)) eventi.removeFirst();
    }
    @Scheduled(fixedDelayString = "${assistenza.rate-limit.pulizia-ms:600000}")
    public void pulisciScaduti() {
        pulisciMappa(ticketCreati, FINESTRA_TICKET);
        pulisciMappa(messaggiInviati, FINESTRA_MESSAGGI);
    }
    private void pulisciMappa(Map<Long, Deque<Instant>> mappa, Duration finestra) {
        mappa.entrySet().removeIf(entry -> {
            Deque<Instant> eventi = entry.getValue();
            synchronized (eventi) {
                rimuoviScaduti(eventi, finestra);
                return eventi.isEmpty();
            }
        });
    }
}
