package it.nutrizionista.restnutrizionista.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.repository.EventoGamificationRepository;
import it.nutrizionista.restnutrizionista.service.GamificationService;

/**
 * Pulizia notturna del log eventi gamification: evita che {@code eventi_gamification} cresca
 * all'infinito. La soglia di retention è la stessa finestra guardata da
 * {@link GamificationService#calcolaStreakGiorni} per lo streak di accessi giornalieri
 * ({@link GamificationService#STREAK_LOOKBACK_GIORNI}, 730 giorni): oltre quella soglia nessuna
 * query del motore gamification legge più questi eventi, quindi è sicuro eliminarli. La pagina
 * "Ultime Attività" non è impattata: mostra solo le ultime 5 voci, già ben dentro la soglia.
 */
@Component
public class GamificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(GamificationCleanupScheduler.class);

    private final EventoGamificationRepository eventoRepo;

    public GamificationCleanupScheduler(EventoGamificationRepository eventoRepo) {
        this.eventoRepo = eventoRepo;
    }

    // Esegue ogni notte alle 03:00
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void pulisciEventiVecchi() {
        Instant soglia = Instant.now().minus(GamificationService.STREAK_LOOKBACK_GIORNI, ChronoUnit.DAYS);
        int eliminati = eventoRepo.deleteByCreatedAtBefore(soglia);
        if (eliminati > 0) {
            log.info("Gamification: eliminati {} eventi più vecchi di {} giorni", eliminati,
                    GamificationService.STREAK_LOOKBACK_GIORNI);
        }
    }
}
