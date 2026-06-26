package it.nutrizionista.restnutrizionista.service;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import it.nutrizionista.restnutrizionista.dto.*;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.security.GoogleTokenVerifier;
import it.nutrizionista.restnutrizionista.security.JwtUtils;
import it.nutrizionista.restnutrizionista.security.UserDetailsServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

/** Logica di autenticazione: login (JWT) e costruzione payload autorizzazioni. */
@Service
public class AuthService {

    @Autowired private AuthenticationManager authManager;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UtenteRepository utenteRepository;
    @Autowired private RuoloRepository ruoloRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private GoogleTokenVerifier googleTokenVerifier;


    /** Esegue il login e costruisce la LoginResponse completa. */

    public LoginResponse login(@Valid LoginRequest req) {
        // 1) Autentica credenziali (esegue la super query una volta sola)
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // 2) Recupera l'utente direttamente dal Principal autenticato! ZERO QUERY!
        UserDetailsServiceImpl.CustomUserDetails userDetails = 
                (UserDetailsServiceImpl.CustomUserDetails) auth.getPrincipal();
        Utente u = userDetails.getUtente();

        // 3) Estrai le authorities (usiamo direttamente quelle caricate da Spring Security)
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // 4) Crea i claims da inserire nel JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorities); // ← QUESTO È FONDAMENTALE!
        claims.put("email", u.getEmail());
        claims.put("nome", u.getNome());
        claims.put("cognome", u.getCognome());

        // 5) Genera JWT CON i permessi
        String token = jwtUtils.generateJwtToken(req.getEmail(), claims);

        // 6) Ruoli (nel tuo dominio: uno solo, ma restituiamo una lista)
        List<RuoloDto> ruoli = u.getRuolo() != null
                ? List.of(DtoMapper.toRuoloDtoLight(u.getRuolo()))
                : List.of();

        // 7) Permessi dal ruolo (dedupe per alias)
        
        List<PermessoDto> permessi = u.getRuolo() != null
                ? u.getRuolo().getRuoloPermessi().stream()
                    .map(rp -> DtoMapper.toPermessoDtoLight(rp.getPermesso()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(PermessoDto::getAlias, p -> p, (a, b) -> a),
                            m -> new ArrayList<>(m.values())))
                : List.of();

        // 8) Gruppi dai permessi (DTO già pronti; dedupe per alias)
        
        List<GruppoDto> gruppi = permessi.stream()
                .map(PermessoDto::getGruppo)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(GruppoDto::getAlias, g -> g, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));

        // 9) Costruisci risposta
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        //resp.setEmail(u.getEmail());
        //resp.setNome(u.getNome());
        //resp.setCognome(u.getCognome());
        resp.setRuoli(ruoli);
        resp.setPermessi(permessi);
        resp.setGruppi(gruppi);
        return resp;
    }

    /** Logout stateless: qui non serve fare nulla. */

    public void logout() {
        // Se vuoi implementare una blacklist di token, fallo qui.
    }

    /** Registra un nuovo utente con ruolo USER assegnato automaticamente. */

    public Utente register(@Valid RegisterRequest req) {
        // Verifica email esistente
        utenteRepository.findByEmail(req.getEmail()).ifPresent(u -> {
            throw new RuntimeException("Email già registrata");
        });

        // Verifica codice fiscale esistente
        utenteRepository.findByCodiceFiscale(req.getCodiceFiscale()).ifPresent(u -> {
            throw new RuntimeException("Codice fiscale già registrato");
        });

        // Trova ruolo USER
        var ruoloUser = ruoloRepository.findByAlias("NUTRIZIONISTA")
                .orElseThrow(() -> new RuntimeException("Ruolo NUTRIZIONISTA non configurato"));

        // Crea utente
        Utente u = new Utente();
        u.setNome(req.getNome());
        u.setCognome(req.getCognome());
        u.setEmail(req.getEmail());
        u.setCodiceFiscale(req.getCodiceFiscale());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setTelefono(req.getTelefono());
        u.setIndirizzo(req.getIndirizzo());
        u.setRuolo(ruoloUser);

        return utenteRepository.save(u);
    }

    /**
     * Login (o preparazione registrazione) tramite Google Identity Services.
     * Verifica sempre l'idToken lato server: non ci si fida di alcun dato client.
     * Se esiste già un Utente con la stessa email -> login immediato (JWT).
     * Altrimenti -> registrationRequired=true, con email/nome/cognome da Google
     * per precompilare il form di completamento (vedi registerWithGoogle).
     */
    public GoogleAuthResponse loginOrPrepareRegisterWithGoogle(@Valid GoogleIdTokenRequest req) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(req.getIdToken());
        String email = payload.getEmail();
        String[] nomeCognome = extractNomeCognome(payload, email);

        GoogleAuthResponse resp = new GoogleAuthResponse();
        resp.setEmail(email);
        resp.setNome(nomeCognome[0]);
        resp.setCognome(nomeCognome[1]);

        Optional<Utente> existing = utenteRepository.findWithAuthoritiesByEmail(email);
        if (existing.isPresent()) {
            resp.setRegistrationRequired(false);
            resp.setLoginResponse(buildLoginResponseFor(existing.get()));
        } else {
            resp.setRegistrationRequired(true);
        }
        return resp;
    }

    /**
     * Completa la registrazione avviata con Google: richiede i campi obbligatori
     * su Utente che Google non fornisce (codiceFiscale, telefono, indirizzo).
     * Nome/cognome/email vengono letti dal token riverificato, non dal client.
     */
    public LoginResponse registerWithGoogle(@Valid GoogleRegisterRequest req) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(req.getIdToken());
        String email = payload.getEmail();
        String[] nomeCognome = extractNomeCognome(payload, email);

        utenteRepository.findByEmail(email).ifPresent(u -> {
            throw new RuntimeException("Email già registrata");
        });
        utenteRepository.findByCodiceFiscale(req.getCodiceFiscale()).ifPresent(u -> {
            throw new RuntimeException("Codice fiscale già registrato");
        });

        var ruoloUser = ruoloRepository.findByAlias("NUTRIZIONISTA")
                .orElseThrow(() -> new RuntimeException("Ruolo NUTRIZIONISTA non configurato"));

        Utente u = new Utente();
        u.setNome(nomeCognome[0]);
        u.setCognome(nomeCognome[1]);
        u.setEmail(email);
        u.setCodiceFiscale(req.getCodiceFiscale());
        u.setTelefono(req.getTelefono());
        u.setIndirizzo(req.getIndirizzo());
        u.setDataNascita(req.getDataNascita());
        // Password inutilizzabile e mai esposta: questo utente accede sempre via Google.
        // Serve solo a soddisfare il vincolo NOT NULL esistente su Utente.password.
        u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + UUID.randomUUID().toString()));
        u.setRuolo(ruoloUser);

        Utente saved = utenteRepository.save(u);
        Utente withAuthorities = utenteRepository.findWithAuthoritiesByEmail(saved.getEmail())
                .orElseThrow(() -> new RuntimeException("Errore interno: utente appena creato non trovato"));
        return buildLoginResponseFor(withAuthorities);
    }

    /** Estrae nome/cognome dal payload Google, con fallback se given_name/family_name assenti. */
    private String[] extractNomeCognome(GoogleIdToken.Payload payload, String email) {
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");
        if (givenName == null || givenName.isBlank()) {
            String fullName = (String) payload.get("name");
            if (fullName != null && !fullName.isBlank()) {
                String[] parts = fullName.trim().split("\\s+", 2);
                givenName = parts[0];
                familyName = parts.length > 1 ? parts[1] : familyName;
            }
        }
        if (givenName == null || givenName.isBlank()) {
            givenName = email.substring(0, email.indexOf('@'));
        }
        if (familyName == null) {
            familyName = "";
        }
        return new String[] { givenName, familyName };
    }

    /** Costruisce token + ruoli/permessi/gruppi per un Utente già caricato con ruolo/permessi (stessa logica di login()). */
    private LoginResponse buildLoginResponseFor(Utente u) {
        List<String> authorities = u.getRuolo() != null
                ? u.getRuolo().getRuoloPermessi().stream()
                    .map(rp -> rp.getPermesso().getAlias())
                    .collect(Collectors.toList())
                : List.of();

        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorities);
        claims.put("email", u.getEmail());
        claims.put("nome", u.getNome());
        claims.put("cognome", u.getCognome());

        String token = jwtUtils.generateJwtToken(u.getEmail(), claims);

        List<RuoloDto> ruoli = u.getRuolo() != null
                ? List.of(DtoMapper.toRuoloDtoLight(u.getRuolo()))
                : List.of();

        List<PermessoDto> permessi = u.getRuolo() != null
                ? u.getRuolo().getRuoloPermessi().stream()
                    .map(rp -> DtoMapper.toPermessoDtoLight(rp.getPermesso()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(PermessoDto::getAlias, p -> p, (a, b) -> a),
                            m -> new ArrayList<>(m.values())))
                : List.of();

        List<GruppoDto> gruppi = permessi.stream()
                .map(PermessoDto::getGruppo)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(GruppoDto::getAlias, g -> g, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setRuoli(ruoli);
        resp.setPermessi(permessi);
        resp.setGruppi(gruppi);
        return resp;
    }
}
