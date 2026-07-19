package it.nutrizionista.restnutrizionista.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AccountDemoDto;
import it.nutrizionista.restnutrizionista.dto.ClienteAccountDemoDto;
import it.nutrizionista.restnutrizionista.dto.CreaAccountDemoRequest;
import it.nutrizionista.restnutrizionista.dto.ImpersonazioneDemoRequest;
import it.nutrizionista.restnutrizionista.dto.ImpersonazioneDemoResponse;
import it.nutrizionista.restnutrizionista.dto.LoginResponse;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.CredenzialeDemo;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AzioneAuditDemo;
import it.nutrizionista.restnutrizionista.enums.EsitoAuditDemo;
import it.nutrizionista.restnutrizionista.enums.MetodoRegistrazione;
import it.nutrizionista.restnutrizionista.enums.StatoAccountDemo;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.repository.CredenzialeDemoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.security.DemoRateLimitService;

/** Gestione amministrativa degli account demo e impersonazione a privilegi ridotti. */
@Service
public class AdminDemoAccountService {
    private static final Duration DURATA_DEMO = Duration.ofDays(14);
    private static final Duration DURATA_IMPERSONAZIONE = Duration.ofMinutes(15);
    @Autowired private CredenzialeDemoRepository demoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UtenteRepository utenteRepository;
    @Autowired private RuoloRepository ruoloRepository;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DemoPasswordPolicyService passwordPolicy;
    @Autowired private DemoAuditService auditService;
    @Autowired private DemoRateLimitService rateLimitService;
    @Autowired private DemoAuthService demoAuthService;
    @Autowired private Clock clock;

    @Value("${demo.master-password-hash:}")
    private String masterPasswordHash;

    @Transactional
    public AccountDemoDto crea(CreaAccountDemoRequest request, String ip, String userAgent) {
        Utente admin = currentUserService.getMe();
        String username = normalizzaUsername(request.getUsername());
        passwordPolicy.valida(request.getPassword());
        if (demoRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username demo già utilizzato");
        }
        Ruolo ruolo = ruoloRepository.findByAlias("NUTRIZIONISTA")
                .orElseThrow(() -> new IllegalStateException("Ruolo NUTRIZIONISTA non configurato"));
        String random = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        Utente utente = new Utente();
        utente.setNome(username);
        utente.setCognome("Demo");
        utente.setEmail("demo-" + UUID.randomUUID() + "@accounts.invalid");
        utente.setCodiceFiscale("DM" + random.substring(0, 14));
        utente.setTelefono("-");
        utente.setIndirizzo("Account demo");
        // Password tecnica casuale: il login standard non può usare la password demo.
        utente.setPassword(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
        utente.setRuolo(ruolo);
        utente.setMetodoRegistrazione(MetodoRegistrazione.DEMO);
        utente = utenteRepository.save(utente);

        CredenzialeDemo demo = new CredenzialeDemo();
        demo.setUtente(utente);
        demo.setUsername(username);
        demo.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        demo.setExpiresAt(clock.instant().plus(DURATA_DEMO));
        demo.setCreatedByAdminId(admin.getId());
        demo = demoRepository.saveAndFlush(demo);
        auditService.recordSameTx(admin.getId(), utente.getId(), demo.getId(), AzioneAuditDemo.CREAZIONE,
                EsitoAuditDemo.SUCCESSO, "Creazione account demo 14 giorni", ip, userAgent);
        return toDto(demo, clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountDemoDto> lista(StatoAccountDemo stato, int page, int size) {
        int pagina = Math.max(0, page);
        int dimensione = Math.max(1, Math.min(size, 50));
        PageRequest pageable = PageRequest.of(pagina, dimensione);
        Instant now = clock.instant();
        Page<CredenzialeDemo> risultati;
        if (stato == null) {
            risultati = demoRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            risultati = switch (stato) {
                case ATTIVO -> demoRepository.findByDisabledAtIsNullAndExpiresAtGreaterThanOrderByCreatedAtDesc(now, pageable);
                case SCADUTO -> demoRepository.findByDisabledAtIsNullAndExpiresAtLessThanEqualOrderByCreatedAtDesc(now, pageable);
                case DISABILITATO -> demoRepository.findByDisabledAtIsNotNullOrderByCreatedAtDesc(pageable);
            };
        }
        Map<Long, Long> clientiPerNutrizionista = contaClienti(risultati);
        return PageResponse.from(risultati.map(demo ->
                toDto(demo, now, clientiPerNutrizionista.getOrDefault(demo.getUtente().getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClienteAccountDemoDto> listaClienti(
            Long demoId, String query, int page, int size) {
        int pagina = Math.max(0, page);
        int dimensione = Math.max(1, Math.min(size, 50));
        CredenzialeDemo demo = demoRepository.findConUtenteById(demoId)
                .orElseThrow(() -> new NotFoundException("Account demo non trovato"));
        PageRequest pageable = PageRequest.of(pagina, dimensione);
        String ricerca = query == null ? "" : query.trim();
        if (ricerca.length() > 100) {
            throw new BadRequestException("La ricerca clienti non puo superare 100 caratteri");
        }
        Page<it.nutrizionista.restnutrizionista.entity.Cliente> clienti = ricerca.isEmpty()
                ? clienteRepository.findByNutrizionista_IdOrderByCreatedAtDesc(
                        demo.getUtente().getId(), pageable)
                : clienteRepository.searchForDemoAdmin(
                        demo.getUtente().getId(), ricerca, pageable);
        return PageResponse.from(clienti.map(this::toClienteDto));
    }

    @Transactional
    public AccountDemoDto disabilita(Long id, String ip, String userAgent) {
        return cambiaAbilitazione(id, true, ip, userAgent);
    }

    @Transactional
    public AccountDemoDto riabilita(Long id, String ip, String userAgent) {
        return cambiaAbilitazione(id, false, ip, userAgent);
    }

    private AccountDemoDto cambiaAbilitazione(Long id, boolean disabilita, String ip, String userAgent) {
        Utente admin = currentUserService.getMe();
        CredenzialeDemo demo = getLocked(id);
        demo.setDisabledAt(disabilita ? clock.instant() : null);
        demo.setTokenVersion(demo.getTokenVersion() + 1);
        demoRepository.saveAndFlush(demo);
        auditService.recordSameTx(admin.getId(), demo.getUtente().getId(), demo.getId(),
                disabilita ? AzioneAuditDemo.DISABILITAZIONE : AzioneAuditDemo.RIABILITAZIONE,
                EsitoAuditDemo.SUCCESSO, null, ip, userAgent);
        return toDto(demo, clock.instant());
    }

    @Transactional
    public AccountDemoDto estendi(Long id, int giorni, String ip, String userAgent) {
        Utente admin = currentUserService.getMe();
        if (giorni < 1 || giorni > 30) throw new BadRequestException("Estensione consentita: 1-30 giorni");
        CredenzialeDemo demo = getLocked(id);
        Instant base = demo.getExpiresAt().isAfter(clock.instant()) ? demo.getExpiresAt() : clock.instant();
        demo.setExpiresAt(base.plus(Duration.ofDays(giorni)));
        demo.setTokenVersion(demo.getTokenVersion() + 1);
        demoRepository.saveAndFlush(demo);
        auditService.recordSameTx(admin.getId(), demo.getUtente().getId(), demo.getId(),
                AzioneAuditDemo.ESTENSIONE, EsitoAuditDemo.SUCCESSO, "Estensione di " + giorni + " giorni", ip, userAgent);
        return toDto(demo, clock.instant());
    }

    @Transactional
    public AccountDemoDto ruotaPassword(Long id, String password, String ip, String userAgent) {
        Utente admin = currentUserService.getMe();
        passwordPolicy.valida(password);
        CredenzialeDemo demo = getLocked(id);
        demo.setPasswordHash(passwordEncoder.encode(password));
        demo.setTokenVersion(demo.getTokenVersion() + 1);
        demoRepository.saveAndFlush(demo);
        auditService.recordSameTx(admin.getId(), demo.getUtente().getId(), demo.getId(),
                AzioneAuditDemo.ROTAZIONE_PASSWORD, EsitoAuditDemo.SUCCESSO, null, ip, userAgent);
        return toDto(demo, clock.instant());
    }

    @Transactional
    public AccountDemoDto revocaSessioni(Long id, String ip, String userAgent) {
        Utente admin = currentUserService.getMe();
        CredenzialeDemo demo = getLocked(id);
        demo.setTokenVersion(demo.getTokenVersion() + 1);
        demoRepository.saveAndFlush(demo);
        auditService.recordSameTx(admin.getId(), demo.getUtente().getId(), demo.getId(),
                AzioneAuditDemo.REVOCA_SESSIONI, EsitoAuditDemo.SUCCESSO, null, ip, userAgent);
        return toDto(demo, clock.instant());
    }

    @Transactional
    public ImpersonazioneDemoResponse impersona(Long id, ImpersonazioneDemoRequest request,
            String ip, String userAgent) {
        Utente admin = currentUserService.getMe();
        rateLimitService.verificaImpersonazione(admin.getId(), ip);
        CredenzialeDemo demo = getLocked(id);
        if (demo.getDisabledAt() != null) {
            throw new BadRequestException(
                    "Account demo disabilitato: riabilitalo prima dell'accesso assistito");
        }
        boolean adminValido = passwordEncoder.matches(request.getAdminPassword(), admin.getPassword());
        boolean masterConfigurata = masterPasswordHash != null && !masterPasswordHash.isBlank();
        boolean masterValida = masterConfigurata
                && passwordEncoder.matches(request.getMasterPassword(), masterPasswordHash);
        if (!(adminValido & masterValida)) {
            rateLimitService.fallimentoImpersonazione(admin.getId(), ip);
            auditService.recordNewTx(admin.getId(), demo.getUtente().getId(), demo.getId(),
                    AzioneAuditDemo.IMPERSONAZIONE_FALLITA, EsitoAuditDemo.NEGATO,
                    request.getMotivo(), ip, userAgent);
            throw new BadCredentialsException("Autorizzazione amministrativa non valida");
        }
        rateLimitService.successoImpersonazione(admin.getId(), ip);
        Instant scadenzaToken = clock.instant().plus(DURATA_IMPERSONAZIONE);
        LoginResponse login = demoAuthService.creaRisposta(
                demo.getUtente(), demo, true, admin.getId(), scadenzaToken);
        auditService.recordSameTx(admin.getId(), demo.getUtente().getId(), demo.getId(),
                AzioneAuditDemo.IMPERSONAZIONE_RIUSCITA, EsitoAuditDemo.SUCCESSO,
                request.getMotivo(), ip, userAgent);
        ImpersonazioneDemoResponse response = new ImpersonazioneDemoResponse();
        response.setToken(login.getToken());
        response.setUsername(demo.getUsername());
        response.setExpiresAt(scadenzaToken);
        return response;
    }

    private CredenzialeDemo getLocked(Long id) {
        return demoRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Account demo non trovato"));
    }
    private String normalizzaUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
    private AccountDemoDto toDto(CredenzialeDemo demo, Instant now) {
        return toDto(demo, now, clienteRepository.countByNutrizionista_Id(demo.getUtente().getId()));
    }

    private AccountDemoDto toDto(CredenzialeDemo demo, Instant now, long numeroClientiRegistrati) {
        AccountDemoDto dto = new AccountDemoDto();
        dto.setId(demo.getId());
        dto.setUtenteId(demo.getUtente().getId());
        dto.setUsername(demo.getUsername());
        dto.setStato(demo.getDisabledAt() != null ? StatoAccountDemo.DISABILITATO
                : demo.getExpiresAt().isAfter(now) ? StatoAccountDemo.ATTIVO : StatoAccountDemo.SCADUTO);
        dto.setExpiresAt(demo.getExpiresAt());
        dto.setCreatedAt(demo.getCreatedAt());
        dto.setLastLoginAt(demo.getUtente().getLastLoginAt());
        dto.setCreatedByAdminId(demo.getCreatedByAdminId());
        dto.setNumeroClientiRegistrati(numeroClientiRegistrati);
        return dto;
    }

    private Map<Long, Long> contaClienti(Page<CredenzialeDemo> demoPage) {
        var nutrizionistaIds = demoPage.getContent().stream()
                .map(demo -> demo.getUtente().getId())
                .toList();
        if (nutrizionistaIds.isEmpty()) return Map.of();
        return clienteRepository.countByNutrizionistaIds(nutrizionistaIds).stream()
                .collect(Collectors.toMap(
                        ClienteRepository.ConteggioClientiPerNutrizionista::getNutrizionistaId,
                        ClienteRepository.ConteggioClientiPerNutrizionista::getTotale));
    }

    private ClienteAccountDemoDto toClienteDto(it.nutrizionista.restnutrizionista.entity.Cliente cliente) {
        return new ClienteAccountDemoDto(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCognome(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getCreatedAt(),
                cliente.getUpdatedAt(),
                cliente.isTrattamentoLimitato());
    }
}
