package it.nutrizionista.restnutrizionista.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.entity.Permesso;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.RuoloPermesso;
import it.nutrizionista.restnutrizionista.repository.PermessoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloPermessoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;

/** Seeder idempotente del permesso di assistenza per il ruolo nutrizionista. */
@Component
public class AssistenzaSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AssistenzaSeeder.class);
    public static final String ASSISTENZA_USE = "ASSISTENZA_USE";
    @Autowired private PermessoRepository permessoRepository;
    @Autowired private RuoloRepository ruoloRepository;
    @Autowired private RuoloPermessoRepository ruoloPermessoRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Permesso permesso = permessoRepository.findByAlias(ASSISTENZA_USE).orElseGet(() -> {
            Permesso nuovo = new Permesso();
            nuovo.setAlias(ASSISTENZA_USE);
            nuovo.setNome("Richiedi assistenza");
            log.info("Permesso ASSISTENZA_USE creato");
            return permessoRepository.save(nuovo);
        });
        Ruolo ruolo = ruoloRepository.findByAlias("NUTRIZIONISTA").orElse(null);
        if (ruolo == null) {
            log.warn("Permesso ASSISTENZA_USE non associato: ruolo NUTRIZIONISTA non trovato");
            return;
        }
        if (!ruoloPermessoRepository.existsByRuolo_IdAndPermesso_Id(ruolo.getId(), permesso.getId())) {
            ruoloPermessoRepository.save(new RuoloPermesso(ruolo, permesso));
            log.info("Permesso ASSISTENZA_USE associato al ruolo NUTRIZIONISTA");
        } else {
            log.info("Permesso ASSISTENZA_USE già configurato");
        }
    }
}
