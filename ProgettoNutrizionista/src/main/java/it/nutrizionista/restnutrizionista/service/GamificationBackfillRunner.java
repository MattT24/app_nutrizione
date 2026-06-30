package it.nutrizionista.restnutrizionista.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.ProgressioneNutrizionista;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.ProgressioneNutrizionistaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

/**
 * Backfill one-off dei punti gamification per i nutrizionisti già attivi prima del rilascio
 * di questa feature: senza questo passaggio partirebbero tutti da 0 punti nonostante anni di
 * attività già svolta. I badge non necessitano di backfill perché valutati su conteggi reali
 * (vedi {@link GamificationService}), quindi si sbloccano da soli al primo evento successivo.
 *
 * Si esegue una sola volta: appena la tabella progressione_nutrizionista non è più vuota, i
 * riavvii successivi saltano il backfill. Niente {@code @Transactional} a livello di metodo:
 * ogni {@code save()} è quindi una sua transazione indipendente (comportamento di default di
 * Spring Data), così se due istanze partono in contemporanea e collidono sullo stesso
 * nutrizionista, solo quel singolo salvataggio fallisce (catturato e ignorato) invece di far
 * fallire l'intero backfill o, peggio, l'avvio dell'istanza.
 */
@Component
public class GamificationBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GamificationBackfillRunner.class);

    @Autowired private ProgressioneNutrizionistaRepository progressioneRepo;
    @Autowired private UtenteRepository utenteRepo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private SchedaRepository schedaRepo;
    @Autowired private MisurazioneAntropometricaRepository misurazioneRepo;
    @Autowired private AppuntamentoRepository appuntamentoRepo;

    @Override
    public void run(String... args) {
        if (progressioneRepo.count() > 0) {
            return; // backfill già eseguito in un avvio precedente
        }

        List<Utente> nutrizionisti = utenteRepo.findAll();
        if (nutrizionisti.isEmpty()) {
            return;
        }

        log.info("Gamification: backfill punti storici per {} utenti...", nutrizionisti.size());
        int aggiornati = 0;
        for (Utente u : nutrizionisti) {
            long numClienti = clienteRepo.countByNutrizionista_Id(u.getId());
            long numSchede = schedaRepo.countByCliente_Nutrizionista_Id(u.getId());
            long numMisurazioni = misurazioneRepo.countByCliente_Nutrizionista_Id(u.getId());
            long numAppuntamentiCompletati = appuntamentoRepo.countByNutrizionista_IdAndStato(
                    u.getId(), Appuntamento.StatoAppuntamento.COMPLETATO);

            int puntiStorici = (int) (numClienti * 20 + numSchede * 10 + numMisurazioni * 5 + numAppuntamentiCompletati * 8);
            if (puntiStorici == 0) {
                continue;
            }

            try {
                ProgressioneNutrizionista progressione = new ProgressioneNutrizionista();
                progressione.setNutrizionista(u);
                progressione.setPuntiTotali(puntiStorici);
                progressioneRepo.save(progressione);
                aggiornati++;
            } catch (DataIntegrityViolationException e) {
                log.warn("Gamification: backfill per il nutrizionista {} saltato (già presente)", u.getId());
            }
        }
        log.info("Gamification: backfill completato, {} nutrizionisti aggiornati.", aggiornati);
    }
}
