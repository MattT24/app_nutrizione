package it.nutrizionista.restnutrizionista.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.entity.Permesso;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.RuoloPermesso;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.PermessoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloPermessoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

/**
 * Seeder idempotente eseguito ad ogni avvio: garantisce l'esistenza di
 * permesso SUPER_ADMIN, ruolo SUPER_ADMIN e dell'unico utente super admin.
 *
 * Sicurezza:
 * - la password NON è mai presente in chiaro: si configura solo l'hash BCrypt
 *   (superadmin.password-hash, sovrascrivibile con env SUPERADMIN_PASSWORD_HASH);
 * - se l'hash non è configurato, l'utente non viene creato (nessun account di default debole);
 * - il ruolo SUPER_ADMIN non è assegnabile dalla registrazione pubblica, che
 *   assegna sempre e solo il ruolo NUTRIZIONISTA (vedi AuthService.register).
 */
@Component
public class SuperAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    public static final String SUPER_ADMIN_ALIAS = "SUPER_ADMIN";

    @Autowired private RuoloRepository ruoloRepository;
    @Autowired private PermessoRepository permessoRepository;
    @Autowired private RuoloPermessoRepository ruoloPermessoRepository;
    @Autowired private UtenteRepository utenteRepository;

    @Value("${superadmin.email:}")
    private String superAdminEmail;

    @Value("${superadmin.password-hash:}")
    private String superAdminPasswordHash;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Permesso permesso = permessoRepository.findByAlias(SUPER_ADMIN_ALIAS).orElseGet(() -> {
            Permesso p = new Permesso();
            p.setNome("Super Admin");
            p.setAlias(SUPER_ADMIN_ALIAS);
            return permessoRepository.save(p);
        });

        Ruolo ruolo = ruoloRepository.findByAlias(SUPER_ADMIN_ALIAS).orElseGet(() -> {
            Ruolo r = new Ruolo();
            r.setNome("Super Admin");
            r.setAlias(SUPER_ADMIN_ALIAS);
            return ruoloRepository.save(r);
        });

        if (!ruoloPermessoRepository.existsByRuolo_IdAndPermesso_Id(ruolo.getId(), permesso.getId())) {
            ruoloPermessoRepository.save(new RuoloPermesso(ruolo, permesso));
        }

        if (superAdminEmail.isBlank() || superAdminPasswordHash.isBlank()) {
            log.warn("Super admin non creato: configurare superadmin.email e superadmin.password-hash");
            return;
        }

        utenteRepository.findByEmail(superAdminEmail).ifPresentOrElse(u -> {
            // L'account esiste già: ci assicuriamo solo che abbia il ruolo corretto.
            if (u.getRuolo() == null || !SUPER_ADMIN_ALIAS.equals(u.getRuolo().getAlias())) {
                u.setRuolo(ruolo);
                utenteRepository.save(u);
                log.info("Ruolo SUPER_ADMIN riassegnato all'utente super admin");
            }
        }, () -> {
            Utente u = new Utente();
            u.setNome("Super");
            u.setCognome("Admin");
            u.setEmail(superAdminEmail);
            // Valore fittizio ma univoco: il super admin non è una persona fisica censita.
            u.setCodiceFiscale("SUPERADMIN0000000");
            u.setTelefono("-");
            u.setIndirizzo("-");
            // Hash BCrypt già pronto: NON va ricodificato.
            u.setPassword(superAdminPasswordHash);
            u.setRuolo(ruolo);
            utenteRepository.save(u);
            log.info("Utente super admin creato ({})", superAdminEmail);
        });
    }
}
