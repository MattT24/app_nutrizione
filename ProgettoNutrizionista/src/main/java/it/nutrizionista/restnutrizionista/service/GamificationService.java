package it.nutrizionista.restnutrizionista.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.BadgeDto;
import it.nutrizionista.restnutrizionista.dto.CategoriaProgressoDto;
import it.nutrizionista.restnutrizionista.dto.GamificationEventoDto;
import it.nutrizionista.restnutrizionista.dto.GamificationStatoDto;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.BadgeSbloccato;
import it.nutrizionista.restnutrizionista.entity.EventoGamification;
import it.nutrizionista.restnutrizionista.entity.MeseGratisRiscattato;
import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.ProgressioneNutrizionista;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.BadgeSbloccatoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.EventoGamificationRepository;
import it.nutrizionista.restnutrizionista.repository.MeseGratisRiscattatoRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.OrariStudioRepository;
import it.nutrizionista.restnutrizionista.repository.ProgressioneNutrizionistaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;

/**
 * Motore gamification per il nutrizionista: registra gli eventi che generano punti,
 * mantiene il totale punti/livello e valuta lo sblocco di nuovi badge. Nessun endpoint
 * di scrittura è esposto pubblicamente: i punti si generano solo lato server in risposta
 * ad azioni reali già autenticate/autorizzate (vedi hook nei service di dominio).
 *
 * {@link #registraEvento} è chiamato dentro la stessa transazione dell'operazione di business
 * che lo scatena (creare un cliente, completare un appuntamento, fare login, ...): per questo
 * non deve mai poter fare fallire quell'operazione. Gira quindi in una transazione propria
 * ({@code REQUIRES_NEW}) e qualunque eccezione viene loggata e ignorata, mai propagata.
 */
@Service
public class GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);

    /** Costo in punti riscattabili di un mese gratis di abbonamento. */
    public static final int SOGLIA_MESE_GRATIS = 5000;

    /**
     * Finestra massima (in giorni) guardata indietro per lo streak di accessi giornalieri.
     * Usata anche da {@code GamificationCleanupScheduler} per sapere quali eventi sono
     * sicuramente non più necessari: oltre questa soglia nessuna query li legge più.
     */
    public static final long STREAK_LOOKBACK_GIORNI = 730;

    @Autowired private EventoGamificationRepository eventoRepo;
    @Autowired private ProgressioneNutrizionistaRepository progressioneRepo;
    @Autowired private BadgeSbloccatoRepository badgeRepo;
    @Autowired private MeseGratisRiscattatoRepository meseGratisRepo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private SchedaRepository schedaRepo;
    @Autowired private MisurazioneAntropometricaRepository misurazioneRepo;
    @Autowired private AppuntamentoRepository appuntamentoRepo;
    @Autowired private OrariStudioRepository orariStudioRepo;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private EmailService emailService;

    /** Self-injection: serve per richiamare {@link #registraEventoTransazionale} attraverso il
     *  proxy Spring (altrimenti la sua {@code @Transactional(REQUIRES_NEW)} verrebbe ignorata,
     *  essendo una chiamata interna alla stessa istanza). */
    @Autowired @Lazy private GamificationService self;

    /**
     * Registra un evento gamification per il nutrizionista indicato. Non lancia mai eccezioni:
     * un problema qui (DB, race su un badge, ecc.) non deve mai far fallire l'azione reale che
     * lo ha scatenato (creare un cliente, fare login, ...).
     */
    public void registraEvento(Utente nutrizionista, TipoEventoGamification tipo, Long clienteId) {
        try {
            self.registraEventoTransazionale(nutrizionista, tipo, clienteId);
        } catch (Exception e) {
            log.error("Gamification: errore registrando l'evento {} per il nutrizionista {}",
                    tipo, nutrizionista.getId(), e);
        }
    }

    /**
     * Per {@link TipoEventoGamification#ACCESSO_GIORNALIERO} è idempotente: al più un evento al
     * giorno. Gira in una transazione separata ({@code REQUIRES_NEW}) rispetto a quella del
     * chiamante, così un eventuale rollback qui non si propaga all'operazione di business.
     *
     * I punti totali (usati per i livelli) crescono ad ogni evento, senza limiti: registrare
     * 10 misurazioni in un giorno vale comunque 10 eventi pieni ai fini del livello. I punti
     * riscattabili (usati per i premi, es. il mese gratis) crescono invece solo per il primo
     * evento di un certo tipo nella giornata: altrimenti basterebbe spammare azioni (es. tante
     * misurazioni di fila) per gonfiare il saldo spendibile senza un reale corrispettivo d'uso.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void registraEventoTransazionale(Utente nutrizionista, TipoEventoGamification tipo, Long clienteId) {
        Instant inizioGiorno = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        boolean giaRegistratoOggi = eventoRepo.existsByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                nutrizionista.getId(), tipo, inizioGiorno);

        if (tipo == TipoEventoGamification.ACCESSO_GIORNALIERO && giaRegistratoOggi) {
            return; // idempotente: al più un accesso giornaliero al giorno
        }

        EventoGamification evento = new EventoGamification();
        evento.setNutrizionista(nutrizionista);
        evento.setTipoEvento(tipo);
        evento.setPunti(tipo.getPunti());
        evento.setClienteId(clienteId);
        eventoRepo.save(evento);

        // Incremento atomico (UPDATE diretto): evita di perdere punti se due eventi per lo
        // stesso nutrizionista vengono registrati in concorrenza (niente read-modify-write).
        int deltaRiscattabili = giaRegistratoOggi ? 0 : tipo.getPunti();
        int righeAggiornate = progressioneRepo.incrementaPunti(nutrizionista.getId(), tipo.getPunti(), deltaRiscattabili);
        if (righeAggiornate == 0) {
            ProgressioneNutrizionista nuova = new ProgressioneNutrizionista();
            nuova.setNutrizionista(nutrizionista);
            nuova.setPuntiTotali(tipo.getPunti());
            nuova.setPuntiRiscattabili(deltaRiscattabili);
            progressioneRepo.save(nuova);
        }

        valutaNuoviBadge(nutrizionista, gruppoPer(tipo));
    }

    /** Gruppo di {@link GamificationBadgeCatalogo} interessato da un tipo di evento. */
    private static GruppoBadge gruppoPer(TipoEventoGamification tipo) {
        return switch (tipo) {
            case NUOVO_CLIENTE -> GruppoBadge.CLIENTI;
            case SCHEDA_CREATA -> GruppoBadge.SCHEDE;
            case MISURAZIONE_REGISTRATA -> GruppoBadge.MISURAZIONI;
            case APPUNTAMENTO_COMPLETATO -> GruppoBadge.APPUNTAMENTI;
            case ACCESSO_GIORNALIERO -> GruppoBadge.COSTANZA;
        };
    }

    /**
     * Valuta lo sblocco di nuovi badge, ma solo per il gruppo interessato dall'evento appena
     * registrato: registrare una misurazione non ha senso che faccia ricalcolare clienti, schede,
     * appuntamenti e streak. Evita così 4 query + il caricamento dello storico accessi ad ogni
     * singola scrittura di dominio (cliente/scheda/misurazione/appuntamento), quando in realtà
     * solo uno di quei contatori può essere cambiato.
     */
    private void valutaNuoviBadge(Utente nutrizionista, GruppoBadge gruppo) {
        long valoreGruppo = calcolaContatoreGruppo(nutrizionista.getId(), gruppo);
        GamificationContatori contatori = contatoriPerGruppo(gruppo, valoreGruppo);

        Set<String> giaSbloccati = badgeRepo.findByNutrizionista_Id(nutrizionista.getId())
                .stream()
                .map(BadgeSbloccato::getCodiceBadge)
                .collect(Collectors.toSet());

        for (GamificationBadgeDefinizione def : GamificationBadgeCatalogo.TUTTI) {
            if (!def.gruppo().equals(gruppo) || giaSbloccati.contains(def.codice())) {
                continue;
            }
            if (def.sbloccato(contatori)) {
                sbloccaBadge(nutrizionista, def);
            }
        }
    }

    /** Persiste lo sblocco e invia l'email di milestone; ignora la race in cui un'altra richiesta
     *  concorrente ha già sbloccato lo stesso badge (vincolo unique su utente+codice). */
    private void sbloccaBadge(Utente nutrizionista, GamificationBadgeDefinizione def) {
        try {
            BadgeSbloccato sbloccato = new BadgeSbloccato();
            sbloccato.setNutrizionista(nutrizionista);
            sbloccato.setCodiceBadge(def.codice());
            badgeRepo.save(sbloccato);
        } catch (DataIntegrityViolationException e) {
            log.debug("Gamification: badge {} già sbloccato per il nutrizionista {} (race ignorata)",
                    def.codice(), nutrizionista.getId());
            return;
        }

        log.info("Nutrizionista {} ha sbloccato il badge {}", nutrizionista.getId(), def.codice());
        if (nutrizionista.getEmail() != null && !nutrizionista.getEmail().isBlank()) {
            emailService.sendGamificationMilestone(
                    nutrizionista.getEmail(), nutrizionista.getNome(), def.nome(), def.descrizione());
        }
    }

    /**
     * Streak corrente di accessi giornalieri consecutivi (oggi compreso). Si interrompe al primo
     * giorno lavorativo senza accesso scorrendo a ritroso da oggi; limitato ai due anni
     * precedenti per non caricare uno storico illimitato.
     *
     * I giorni in cui lo studio è chiuso (vedi {@code OrariStudio}, es. weekend per molti studi)
     * vengono saltati senza interrompere lo streak: non avrebbe senso pretendere un accesso un
     * giorno in cui il nutrizionista non lavora. Se lo studio non ha mai configurato gli orari
     * per un giorno, quel giorno è considerato lavorativo (stessa convenzione già usata per la
     * validazione degli appuntamenti).
     */
    private int calcolaStreakGiorni(Long nutrizionistaId) {
        Instant da = Instant.now().minus(STREAK_LOOKBACK_GIORNI, ChronoUnit.DAYS);
        LocalDate limite = LocalDate.ofInstant(da, ZoneId.systemDefault());

        Set<LocalDate> giorniConAccesso = eventoRepo
                .findByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                        nutrizionistaId, TipoEventoGamification.ACCESSO_GIORNALIERO, da)
                .stream()
                .map(e -> e.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toCollection(HashSet::new));

        Set<DayOfWeek> giorniChiusura = giorniDiChiusura(nutrizionistaId);

        int streak = 0;
        LocalDate cursore = LocalDate.now();
        while (!cursore.isBefore(limite)) {
            if (giorniConAccesso.contains(cursore)) {
                streak++;
            } else if (!giorniChiusura.contains(cursore.getDayOfWeek())) {
                break; // giorno lavorativo senza accesso: qui lo streak si interrompe
            }
            // giorno di chiusura senza accesso: non richiesto, si salta senza interrompere lo streak
            cursore = cursore.minusDays(1);
        }
        return streak;
    }

    /**
     * Giorni della settimana in cui lo studio del nutrizionista è chiuso, secondo {@code
     * OrariStudio}. In caso di righe duplicate per lo stesso giorno (può capitare, vedi
     * {@code OrariStudioRepository}) vince quella con id più basso, stessa convenzione usata
     * altrove nel codice per risolvere i duplicati in modo deterministico.
     */
    private Set<DayOfWeek> giorniDiChiusura(Long nutrizionistaId) {
        List<OrariStudio> orari = orariStudioRepo.findByNutrizionista_Id(nutrizionistaId);
        Map<DayOfWeek, OrariStudio> primoPerGiorno = orari.stream()
                .sorted(Comparator.comparing(OrariStudio::getId))
                .collect(Collectors.toMap(OrariStudio::getGiornoSettimana, o -> o, (esistente, nuovo) -> esistente));
        return primoPerGiorno.values().stream()
                .filter(o -> !o.isGiornoLavorativo())
                .map(OrariStudio::getGiornoSettimana)
                .collect(Collectors.toSet());
    }

    /** Il singolo contatore rilevante per un gruppo (una sola query, non tutte e 5). */
    private long calcolaContatoreGruppo(Long nutrizionistaId, GruppoBadge gruppo) {
        return switch (gruppo) {
            case CLIENTI -> clienteRepo.countByNutrizionista_Id(nutrizionistaId);
            case SCHEDE -> schedaRepo.countByCliente_Nutrizionista_Id(nutrizionistaId);
            case MISURAZIONI -> misurazioneRepo.countByCliente_Nutrizionista_Id(nutrizionistaId);
            case APPUNTAMENTI -> appuntamentoRepo.countByNutrizionista_IdAndStato(
                    nutrizionistaId, Appuntamento.StatoAppuntamento.COMPLETATO);
            case COSTANZA -> calcolaStreakGiorni(nutrizionistaId);
        };
    }

    /** Contatori "parziali": solo il campo del gruppo richiesto è popolato, gli altri restano a
     *  zero. Va bene perché chi lo usa valuta solo badge di quello stesso gruppo (vedi {@link #valutaNuoviBadge}). */
    private GamificationContatori contatoriPerGruppo(GruppoBadge gruppo, long valore) {
        return switch (gruppo) {
            case CLIENTI -> new GamificationContatori(valore, 0, 0, 0, 0);
            case SCHEDE -> new GamificationContatori(0, valore, 0, 0, 0);
            case MISURAZIONI -> new GamificationContatori(0, 0, valore, 0, 0);
            case APPUNTAMENTI -> new GamificationContatori(0, 0, 0, valore, 0);
            case COSTANZA -> new GamificationContatori(0, 0, 0, 0, (int) valore);
        };
    }

    private GamificationContatori calcolaContatori(Long nutrizionistaId) {
        return new GamificationContatori(
                clienteRepo.countByNutrizionista_Id(nutrizionistaId),
                schedaRepo.countByCliente_Nutrizionista_Id(nutrizionistaId),
                misurazioneRepo.countByCliente_Nutrizionista_Id(nutrizionistaId),
                appuntamentoRepo.countByNutrizionista_IdAndStato(nutrizionistaId, Appuntamento.StatoAppuntamento.COMPLETATO),
                calcolaStreakGiorni(nutrizionistaId));
    }

    private static final Map<GruppoBadge, String> TITOLO_PER_GRUPPO = Map.of(
            GruppoBadge.CLIENTI, "Clienti",
            GruppoBadge.SCHEDE, "Schede Dieta",
            GruppoBadge.MISURAZIONI, "Misurazioni",
            GruppoBadge.APPUNTAMENTI, "Appuntamenti",
            GruppoBadge.COSTANZA, "Costanza");

    private static final String[] NOMI_TIER = {"bronzo", "argento", "oro"};

    /**
     * Un riquadro per gruppo (non uno per ogni Bronzo/Argento/Oro): valore attuale del
     * contatore, tier già raggiunto (se presente) e soglia/nome del prossimo tier da
     * sbloccare. Permette al frontend di mostrare un badge che "sale di livello" invece di
     * tre badge separati, ed è già "attivo" (valoreAttuale > 0) anche prima del primo tier.
     */
    private List<CategoriaProgressoDto> calcolaProgressiCategorie(GamificationContatori contatori) {
        Map<GruppoBadge, List<GamificationBadgeDefinizione>> perGruppo = GamificationBadgeCatalogo.TUTTI.stream()
                .collect(Collectors.groupingBy(GamificationBadgeDefinizione::gruppo, LinkedHashMap::new, Collectors.toList()));

        List<CategoriaProgressoDto> risultato = new ArrayList<>();
        for (var voce : perGruppo.entrySet()) {
            List<GamificationBadgeDefinizione> tiers = voce.getValue();
            long valoreAttuale = tiers.get(0).valore().applyAsLong(contatori);

            String tierAttuale = null;
            GamificationBadgeDefinizione prossimo = null;
            for (int i = 0; i < tiers.size(); i++) {
                GamificationBadgeDefinizione def = tiers.get(i);
                if (def.sbloccato(contatori)) {
                    tierAttuale = NOMI_TIER[i];
                } else if (prossimo == null) {
                    prossimo = def;
                }
            }

            risultato.add(new CategoriaProgressoDto(
                    voce.getKey().name(),
                    TITOLO_PER_GRUPPO.getOrDefault(voce.getKey(), voce.getKey().name()),
                    tiers.get(0).icona(),
                    valoreAttuale,
                    tierAttuale,
                    prossimo != null ? prossimo.soglia() : null,
                    prossimo != null ? prossimo.nome() : null));
        }
        return risultato;
    }

    /**
     * Riscatta un mese gratis di abbonamento scalando {@link #SOGLIA_MESE_GRATIS} punti dal
     * saldo riscattabile del nutrizionista loggato. Non esiste ancora un sistema di
     * abbonamento/billing reale: la riga creata in {@code mesi_gratis_riscattati} è per ora un
     * "buono" da applicare manualmente, pronto per essere collegato a un sistema di pagamento
     * vero in futuro.
     */
    @Transactional
    public GamificationStatoDto riscattaMeseGratis() {
        Utente me = currentUserService.getMe();
        int righeAggiornate = progressioneRepo.scalaPuntiRiscattabili(me.getId(), SOGLIA_MESE_GRATIS);
        if (righeAggiornate == 0) {
            throw new ConflictException(
                    "Punti riscattabili insufficienti: servono almeno " + SOGLIA_MESE_GRATIS + " punti.");
        }

        MeseGratisRiscattato riscatto = new MeseGratisRiscattato();
        riscatto.setNutrizionista(me);
        riscatto.setPuntiSpesi(SOGLIA_MESE_GRATIS);
        meseGratisRepo.save(riscatto);
        log.info("Nutrizionista {} ha riscattato un mese gratis ({} punti)", me.getId(), SOGLIA_MESE_GRATIS);

        return getStatoPerMe();
    }

    /** Stato gamification (punti, livello, badge) del nutrizionista loggato. */
    @Transactional(readOnly = true)
    public GamificationStatoDto getStatoPerMe() {
        Utente me = currentUserService.getMe();
        ProgressioneNutrizionista progressione = progressioneRepo.findByNutrizionista_Id(me.getId()).orElse(null);
        int puntiTotali = progressione != null ? progressione.getPuntiTotali() : 0;
        int puntiRiscattabili = progressione != null ? progressione.getPuntiRiscattabili() : 0;

        GamificationLivello attuale = GamificationLivelloCatalogo.attualePer(puntiTotali);
        GamificationLivello successivo = GamificationLivelloCatalogo.successivoPer(puntiTotali);

        double progressoPercentuale;
        Integer puntiPerProssimo;
        if (successivo == null) {
            progressoPercentuale = 100.0;
            puntiPerProssimo = null;
        } else {
            int range = successivo.soglia() - attuale.soglia();
            progressoPercentuale = range <= 0 ? 100.0 : Math.min(100.0, 100.0 * (puntiTotali - attuale.soglia()) / range);
            puntiPerProssimo = successivo.soglia() - puntiTotali;
        }

        var dataSbloccoByCodice = badgeRepo.findByNutrizionista_Id(me.getId())
                .stream()
                .collect(Collectors.toMap(BadgeSbloccato::getCodiceBadge, BadgeSbloccato::getDataSblocco));

        List<BadgeDto> badge = GamificationBadgeCatalogo.TUTTI.stream()
                .map(def -> new BadgeDto(
                        def.codice(), def.nome(), def.descrizione(), def.icona(),
                        dataSbloccoByCodice.containsKey(def.codice()),
                        dataSbloccoByCodice.get(def.codice())))
                .collect(Collectors.toCollection(ArrayList::new));

        GamificationContatori contatori = calcolaContatori(me.getId());

        return new GamificationStatoDto(
                puntiTotali,
                attuale.nome(),
                successivo != null ? successivo.nome() : null,
                puntiPerProssimo,
                progressoPercentuale,
                contatori.streakGiorni(),
                badge,
                calcolaProgressiCategorie(contatori),
                puntiRiscattabili,
                SOGLIA_MESE_GRATIS,
                meseGratisRepo.countByNutrizionista_Id(me.getId()));
    }

    private static final int LIMIT_STORICO_MASSIMO = 100;

    /** Ultimi eventi punti del nutrizionista loggato (storico per la pagina Traguardi). */
    @Transactional(readOnly = true)
    public List<GamificationEventoDto> getStorico(int limit) {
        Utente me = currentUserService.getMe();
        int limiteSicuro = Math.min(Math.max(limit, 1), LIMIT_STORICO_MASSIMO);
        return eventoRepo.findByNutrizionista_IdOrderByCreatedAtDesc(me.getId(), PageRequest.of(0, limiteSicuro))
                .stream()
                .map(e -> new GamificationEventoDto(e.getTipoEvento().name(), e.getPunti(), e.getCreatedAt()))
                .toList();
    }
}
