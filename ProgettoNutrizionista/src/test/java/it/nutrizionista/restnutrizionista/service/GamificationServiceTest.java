package it.nutrizionista.restnutrizionista.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.nutrizionista.restnutrizionista.dto.CategoriaProgressoDto;
import it.nutrizionista.restnutrizionista.dto.GamificationStatoDto;
import it.nutrizionista.restnutrizionista.entity.EventoGamification;
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
 * Unit test (Mockito, niente Spring/DB) sulla logica più delicata di {@link GamificationService}:
 * il tetto giornaliero sui punti riscattabili (vedi richiesta utente: "prima misurazione del
 * giorno +5 sia ai totali che ai riscattabili, la seconda +5 solo ai totali"), il riscatto del
 * mese gratis, e il calcolo di livello/percentuale/tier badge.
 *
 * {@code registraEventoTransazionale} è invocato direttamente (è {@code protected}, accessibile
 * da questa classe perché nello stesso package): evita la complicazione della self-injection
 * (proxy Spring) usata in produzione solo per ottenere la transazione {@code REQUIRES_NEW}, che
 * qui non serve perché non c'è nessun vero datasource.
 */
@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock private EventoGamificationRepository eventoRepo;
    @Mock private ProgressioneNutrizionistaRepository progressioneRepo;
    @Mock private BadgeSbloccatoRepository badgeRepo;
    @Mock private MeseGratisRiscattatoRepository meseGratisRepo;
    @Mock private ClienteRepository clienteRepo;
    @Mock private SchedaRepository schedaRepo;
    @Mock private MisurazioneAntropometricaRepository misurazioneRepo;
    @Mock private AppuntamentoRepository appuntamentoRepo;
    @Mock private OrariStudioRepository orariStudioRepo;
    @Mock private CurrentUserService currentUserService;
    @Mock private EmailService emailService;

    @InjectMocks
    private GamificationService service;

    private Utente nutrizionista(long id) {
        Utente u = new Utente();
        u.setId(id);
        return u;
    }

    @Test
    void registraEventoTransazionale_primaMisurazioneDelGiorno_incrementaPuntiTotaliERiscattabili() {
        Utente u = nutrizionista(1L);
        when(eventoRepo.existsByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                eq(1L), eq(TipoEventoGamification.MISURAZIONE_REGISTRATA), any())).thenReturn(false);
        when(progressioneRepo.incrementaPunti(1L, 5, 5)).thenReturn(1);

        service.registraEventoTransazionale(u, TipoEventoGamification.MISURAZIONE_REGISTRATA, 42L);

        verify(eventoRepo).save(any(EventoGamification.class));
        verify(progressioneRepo).incrementaPunti(1L, 5, 5);
    }

    @Test
    void registraEventoTransazionale_secondaMisurazioneStessoGiorno_incrementaSoloPuntiTotali() {
        Utente u = nutrizionista(1L);
        when(eventoRepo.existsByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                eq(1L), eq(TipoEventoGamification.MISURAZIONE_REGISTRATA), any())).thenReturn(true);
        when(progressioneRepo.incrementaPunti(1L, 5, 0)).thenReturn(1);

        service.registraEventoTransazionale(u, TipoEventoGamification.MISURAZIONE_REGISTRATA, 42L);

        // L'evento va comunque registrato in storico: solo il delta riscattabile è azzerato.
        verify(eventoRepo).save(any(EventoGamification.class));
        verify(progressioneRepo).incrementaPunti(1L, 5, 0);
    }

    @Test
    void registraEventoTransazionale_accessoGiornalieroGiaRegistratoOggi_nonRegistraNullaDiNuovo() {
        Utente u = nutrizionista(1L);
        when(eventoRepo.existsByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                eq(1L), eq(TipoEventoGamification.ACCESSO_GIORNALIERO), any())).thenReturn(true);

        service.registraEventoTransazionale(u, TipoEventoGamification.ACCESSO_GIORNALIERO, null);

        verify(eventoRepo, never()).save(any());
        verify(progressioneRepo, never()).incrementaPunti(anyLong(), anyInt(), anyInt());
    }

    @Test
    void riscattaMeseGratis_puntiInsufficienti_lanciaConflictExceptionENonSalvaNulla() {
        when(currentUserService.getMe()).thenReturn(nutrizionista(1L));
        when(progressioneRepo.scalaPuntiRiscattabili(1L, GamificationService.SOGLIA_MESE_GRATIS)).thenReturn(0);

        assertThrows(ConflictException.class, () -> service.riscattaMeseGratis());

        verify(meseGratisRepo, never()).save(any());
    }

    @Test
    void riscattaMeseGratis_puntiSufficienti_scalaIlSaldoESalvaIlRiscatto() {
        when(currentUserService.getMe()).thenReturn(nutrizionista(1L));
        when(progressioneRepo.scalaPuntiRiscattabili(1L, GamificationService.SOGLIA_MESE_GRATIS)).thenReturn(1);

        service.riscattaMeseGratis();

        verify(meseGratisRepo).save(argThat(r -> r.getPuntiSpesi() == GamificationService.SOGLIA_MESE_GRATIS));
    }

    @Test
    void getStatoPerMe_duecentoPunti_calcolaLivelloEPercentualeCorretti() {
        Utente u = nutrizionista(1L);
        when(currentUserService.getMe()).thenReturn(u);

        ProgressioneNutrizionista progressione = new ProgressioneNutrizionista();
        progressione.setNutrizionista(u);
        progressione.setPuntiTotali(200);
        progressione.setPuntiRiscattabili(150);
        when(progressioneRepo.findByNutrizionista_Id(1L)).thenReturn(Optional.of(progressione));

        GamificationStatoDto stato = service.getStatoPerMe();

        assertEquals(200, stato.puntiTotali());
        assertEquals("Nutrizionista Junior", stato.livelloAttuale());
        assertEquals("Nutrizionista", stato.livelloSuccessivo());
        // Soglie 100 -> 300: range 200, (200-100)/200*100 = 50%
        assertEquals(50.0, stato.progressoPercentuale(), 0.01);
        assertEquals(150, stato.puntiRiscattabili());
    }

    @Test
    void getStatoPerMe_150misurazioni_categoriaMisurazioniInTierBronzoConProssimoArgento() {
        when(currentUserService.getMe()).thenReturn(nutrizionista(1L));
        when(misurazioneRepo.countByCliente_Nutrizionista_Id(1L)).thenReturn(150L);

        GamificationStatoDto stato = service.getStatoPerMe();

        CategoriaProgressoDto misurazioni = stato.progressiCategorie().stream()
                .filter(c -> c.chiave().equals("MISURAZIONI"))
                .findFirst()
                .orElseThrow();

        assertEquals("bronzo", misurazioni.tierAttuale());
        assertEquals(150L, misurazioni.valoreAttuale());
        assertEquals(300, misurazioni.sogliaProssimoTier());
        assertEquals("Misurazioni Argento", misurazioni.nomeProssimoTier());
    }

    @Test
    void getStatoPerMe_studioChiusoNelWeekend_loStreakNonSiInterrompeNelWeekend() {
        when(currentUserService.getMe()).thenReturn(nutrizionista(1L));

        OrariStudio sabato = new OrariStudio();
        sabato.setId(1L);
        sabato.setGiornoSettimana(DayOfWeek.SATURDAY);
        sabato.setGiornoLavorativo(false);
        OrariStudio domenica = new OrariStudio();
        domenica.setId(2L);
        domenica.setGiornoSettimana(DayOfWeek.SUNDAY);
        domenica.setGiornoLavorativo(false);
        when(orariStudioRepo.findByNutrizionista_Id(1L)).thenReturn(List.of(sabato, domenica));

        // Accesso in ogni giorno lavorativo (lun-ven) degli ultimi 10 giorni di calendario:
        // nessun "buco" reale, solo i weekend chiusi non hanno un accesso registrato.
        List<EventoGamification> eventi = new ArrayList<>();
        LocalDate cursore = LocalDate.now();
        int giorniLavorativiAttesi = 0;
        for (int i = 0; i < 10; i++) {
            if (cursore.getDayOfWeek() != DayOfWeek.SATURDAY && cursore.getDayOfWeek() != DayOfWeek.SUNDAY) {
                EventoGamification evento = new EventoGamification();
                evento.setCreatedAt(cursore.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant());
                eventi.add(evento);
                giorniLavorativiAttesi++;
            }
            cursore = cursore.minusDays(1);
        }
        when(eventoRepo.findByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                eq(1L), eq(TipoEventoGamification.ACCESSO_GIORNALIERO), any())).thenReturn(eventi);

        GamificationStatoDto stato = service.getStatoPerMe();

        assertEquals(giorniLavorativiAttesi, stato.streakGiorni(),
                "Il weekend chiuso non deve interrompere lo streak: vanno contati solo i giorni lavorativi con accesso");
    }

    @Test
    void getStatoPerMe_nessunOrarioConfigurato_assenzaIeriInterrompeComunqueLoStreak() {
        when(currentUserService.getMe()).thenReturn(nutrizionista(1L));
        // Nessun orario configurato per questo nutrizionista: tutti i giorni sono considerati
        // lavorativi di default (stessa convenzione usata per la validazione appuntamenti),
        // quindi il comportamento "classico" (un giorno mancante interrompe lo streak) resta invariato.

        EventoGamification oggi = new EventoGamification();
        oggi.setCreatedAt(Instant.now());
        when(eventoRepo.findByNutrizionista_IdAndTipoEventoAndCreatedAtGreaterThanEqual(
                eq(1L), eq(TipoEventoGamification.ACCESSO_GIORNALIERO), any())).thenReturn(List.of(oggi));

        GamificationStatoDto stato = service.getStatoPerMe();

        assertEquals(1, stato.streakGiorni());
    }
}
