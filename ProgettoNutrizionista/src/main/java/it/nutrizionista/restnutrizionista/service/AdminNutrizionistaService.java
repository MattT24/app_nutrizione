package it.nutrizionista.restnutrizionista.service;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import it.nutrizionista.restnutrizionista.dto.CreaNutrizionistaRequest;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.MetodoRegistrazione;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.security.KeycloakAdminClient;

/**
 * Onboarding admin-invite di un nutrizionista (beta Keycloak). <b>NON</b> {@code @Transactional}: {@code repo.save}
 * committa subito (DB-first-persist) → poi {@code createUser} su Keycloak (fuori tx, sincrono). Se KC fallisce l'Utente
 * PERSISTE (subjectId=NULL, non può loggare) → <b>502</b> + re-POST reinvia ({@code createUser} idempotente). Il vincolo
 * unique su email cattura eventuali race. {@code subjectId=NULL}: {@code linkOrReject} lo timbra al 1° login via email
 * verificata (NON pre-settarlo → bypasserebbe il gate {@code email_verified}). <b>NESSUNA accettazione</b>: catturate al
 * 1° login (gate Wave-3). Dipendenza UNIDIREZIONALE, self-registration pubblica NON riaperta.
 */
@Service
public class AdminNutrizionistaService {

    private final UtenteRepository utenteRepository;
    private final RuoloRepository ruoloRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<KeycloakAdminClient> keycloakAdminClient;
    private final UtenteService utenteService; // foglia canonica di erasure (delete → deleteAccount → hook I10)

    public AdminNutrizionistaService(UtenteRepository utenteRepository, RuoloRepository ruoloRepository,
            PasswordEncoder passwordEncoder, ObjectProvider<KeycloakAdminClient> keycloakAdminClient,
            UtenteService utenteService) {
        this.utenteRepository = utenteRepository;
        this.ruoloRepository = ruoloRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakAdminClient = keycloakAdminClient;
        this.utenteService = utenteService;
    }

    /**
     * Invariante E (a) — erasure control-plane (art. 17): cancella un nutrizionista. Rifiuta un target SUPER_ADMIN
     * (403). `@Transactional`: il `getRuolo().getAlias()` lazy è nella tx e `utenteService.delete` la joina → l'hook I10
     * (afterCommit) parte al commit. Riusa la foglia canonica, NON duplica la logica di erasure.
     */
    @Transactional
    public void elimina(Long id) {
        Utente u = utenteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nutrizionista non trovato"));
        if ("SUPER_ADMIN".equals(u.getRuolo().getAlias())) {
            throw new ForbiddenException("Account non eliminabile da questo endpoint");
        }
        utenteService.delete(id); // → deleteAccount → erasure DB + hook I10 cross-store Keycloak
    }

    public UtenteDto invita(CreaNutrizionistaRequest req) {
        KeycloakAdminClient kc = keycloakAdminClient.getIfAvailable();
        if (kc == null) {
            throw new BadRequestException("Onboarding nutrizionisti disponibile solo in modalità Keycloak");
        }

        Utente u = utenteRepository.findByEmail(req.getEmail()).orElse(null);
        if (u != null && u.getSubjectId() != null) {
            throw new ConflictException("Nutrizionista già attivo con questa email");
        }
        if (u == null) {
            Ruolo ruolo = ruoloRepository.findByAlias("NUTRIZIONISTA")
                    .orElseThrow(() -> new IllegalStateException("Ruolo NUTRIZIONISTA non configurato"));
            u = new Utente();
            u.setNome(req.getNome());
            u.setCognome(req.getCognome());
            u.setEmail(req.getEmail());
            u.setCodiceFiscale(placeholderCf());
            u.setTelefono("-");
            u.setIndirizzo("Da completare");
            // Password inutilizzabile: soddisfa il vincolo NOT NULL; la reale arriva via invito email Keycloak.
            u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + UUID.randomUUID()));
            u.setRuolo(ruolo);
            u.setMetodoRegistrazione(MetodoRegistrazione.EMAIL);
            // subjectId=NULL, NESSUNA accettazione (catturate al 1° login — gate Wave-3).
            u = utenteRepository.save(u); // DB-first (commit immediato: nessuna tx ambiente)
        }

        try {
            kc.createUser(req.getEmail(), req.getNome(), req.getCognome()); // KC create + invito (idempotente)
        } catch (RuntimeException e) {
            // DB-first-persist: l'Utente resta salvato → il re-POST della stessa email reinvia (createUser idempotente).
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Utente creato ma invito Keycloak fallito: ripeti l'invito");
        }
        return DtoMapper.toUtenteDtoLight(u); // light → nessun accesso lazy al ruolo fuori transazione
    }

    /** CF placeholder (16 alfanumerici, prefisso riconoscibile INV) — il CF reale è richiesto al 1° accesso. */
    private static String placeholderCf() {
        return ("INV" + UUID.randomUUID().toString().replace("-", "").toUpperCase()).substring(0, 16);
    }
}
