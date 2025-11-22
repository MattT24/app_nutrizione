package it.nutrizionista.restnutrizionista.service;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.dto.*;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.security.JwtUtils;

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


    /** Esegue il login e costruisce la LoginResponse completa. */

    public LoginResponse login(@Valid LoginRequest req) {
        // 1) Autentica credenziali
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        // 2) Genera JWT
        String token = jwtUtils.generateJwtToken(req.getEmail(), Map.of());

        // 3) Recupera utente con ruolo e permessi già inizializzati
        Utente u = utenteRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // 4) Ruoli (nel tuo dominio: uno solo, ma restituiamo una lista)
        List<RuoloDto> ruoli = u.getRuolo() != null
                ? List.of(DtoMapper.toRuoloDtoLight(u.getRuolo()))
                : List.of();

        // 5) Permessi dal ruolo (dedupe per alias)
        List<PermessoDto> permessi = u.getRuolo() != null
                ? u.getRuolo().getRuoloPermessi().stream()
                    .map(rp -> DtoMapper.toPermessoDtoLight(rp.getPermesso()))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(PermessoDto::getAlias, p -> p, (a, b) -> a),
                            m -> new ArrayList<>(m.values())))
                : List.of();

        // 6) Gruppi dai permessi (DTO già pronti; dedupe per alias)
        List<GruppoDto> gruppi = permessi.stream()
                .map(PermessoDto::getGruppo)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(GruppoDto::getAlias, g -> g, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));

        // 7) Costruisci risposta
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setEmail(u.getEmail());
        resp.setNome(u.getNome());         // NEW
        resp.setCognome(u.getCognome());   // NEW
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
        var ruoloUser = ruoloRepository.findByAlias("USER")
                .orElseThrow(() -> new RuntimeException("Ruolo USER non configurato"));

        // Crea utente
        Utente u = new Utente();
        u.setNome(req.getNome());
        u.setCognome(req.getCognome());
        u.setEmail(req.getEmail());
        u.setCodiceFiscale(req.getCodiceFiscale());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setTelefono(req.getTelefono());
        u.setRuolo(ruoloUser);

        return utenteRepository.save(u);
    }
    
    
}
