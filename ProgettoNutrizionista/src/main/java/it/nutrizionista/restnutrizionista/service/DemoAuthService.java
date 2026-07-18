package it.nutrizionista.restnutrizionista.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.DemoLoginRequest;
import it.nutrizionista.restnutrizionista.dto.GruppoDto;
import it.nutrizionista.restnutrizionista.dto.LoginResponse;
import it.nutrizionista.restnutrizionista.dto.PermessoDto;
import it.nutrizionista.restnutrizionista.dto.RuoloDto;
import it.nutrizionista.restnutrizionista.entity.CredenzialeDemo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.CredenzialeDemoRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.security.DemoRateLimitService;
import it.nutrizionista.restnutrizionista.security.JwtUtils;

/** Autenticazione isolata degli account demo; non usa né modifica il login standard. */
@Service
public class DemoAuthService {
    // Hash BCrypt valido usato soltanto per uniformare il costo quando lo username non esiste.
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO5Zi6K.1VQ9Q.0T6YwM4HhK8M8PM7t6u";
    @Autowired private CredenzialeDemoRepository demoRepository;
    @Autowired private UtenteRepository utenteRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DemoRateLimitService rateLimitService;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private Clock clock;

    @Transactional
    public LoginResponse login(DemoLoginRequest request, String ip) {
        String username = request.getUsername().trim().toLowerCase();
        rateLimitService.verificaLogin(username, ip);
        CredenzialeDemo demo = demoRepository.findByUsernameIgnoreCase(username).orElse(null);
        String hash = demo == null ? DUMMY_HASH : demo.getPasswordHash();
        if (!passwordEncoder.matches(request.getPassword(), hash) || demo == null) {
            rateLimitService.fallimentoLogin(username, ip);
            throw new BadCredentialsException("Credenziali non valide");
        }
        Instant now = clock.instant();
        if (demo.getDisabledAt() != null) {
            rateLimitService.fallimentoLogin(username, ip);
            throw new DisabledException("Account demo non disponibile");
        }
        if (!demo.getExpiresAt().isAfter(now)) {
            rateLimitService.fallimentoLogin(username, ip);
            throw new CredentialsExpiredException("Periodo demo terminato");
        }
        rateLimitService.successoLogin(username, ip);
        Utente utente = demo.getUtente();
        utente.setLastLoginAt(now);
        utenteRepository.save(utente);
        return creaRisposta(utente, demo, false, null, demo.getExpiresAt());
    }

    public LoginResponse creaRisposta(Utente utente, CredenzialeDemo demo, boolean impersonazione,
            Long actorAdminId, Instant expiresAt) {
        List<SimpleGrantedAuthority> granted = utente.getRuolo().getRuoloPermessi().stream()
                .map(rp -> new SimpleGrantedAuthority(rp.getPermesso().getAlias())).toList();
        List<String> authorities = granted.stream().map(SimpleGrantedAuthority::getAuthority).toList();
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorities);
        claims.put("email", utente.getEmail());
        claims.put("nome", utente.getNome());
        claims.put("cognome", utente.getCognome());
        claims.put("accountType", "DEMO");
        claims.put("demoUsername", demo.getUsername());
        claims.put("demoCredentialId", demo.getId());
        claims.put("tokenVersion", demo.getTokenVersion());
        claims.put("demoExpiresAt", demo.getExpiresAt().getEpochSecond());
        claims.put("impersonation", impersonazione);
        if (actorAdminId != null) claims.put("actorAdminId", actorAdminId);

        List<RuoloDto> ruoli = List.of(DtoMapper.toRuoloDtoLight(utente.getRuolo()));
        List<PermessoDto> permessi = utente.getRuolo().getRuoloPermessi().stream()
                .map(rp -> DtoMapper.toPermessoDtoLight(rp.getPermesso()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(PermessoDto::getAlias, p -> p, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));
        List<GruppoDto> gruppi = permessi.stream().map(PermessoDto::getGruppo).filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(GruppoDto::getAlias, g -> g, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));

        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtils.generateJwtToken(utente.getEmail(), claims, expiresAt));
        response.setRuoli(ruoli);
        response.setPermessi(permessi);
        response.setGruppi(gruppi);
        return response;
    }
}
