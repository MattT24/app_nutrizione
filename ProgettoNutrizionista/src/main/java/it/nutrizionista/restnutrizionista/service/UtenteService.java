package it.nutrizionista.restnutrizionista.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import it.nutrizionista.restnutrizionista.dto.LogoRequestDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.ProfiloLightDto;
import it.nutrizionista.restnutrizionista.dto.ResetPasswordDto;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.dto.UtenteFormDto;
import it.nutrizionista.restnutrizionista.dto.UtenteProfileUpdateDto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.security.KeycloakAdminClient;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.CredenzialeDemoRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
// F-USER-DEL: repository dei figli utente-owned non-cascade (erasure account completa)
import it.nutrizionista.restnutrizionista.repository.AccettazioneDocumentoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AttivitaRecenteRepository;
import it.nutrizionista.restnutrizionista.repository.BadgeSbloccatoRepository;
import it.nutrizionista.restnutrizionista.repository.EventoGamificationRepository;
import it.nutrizionista.restnutrizionista.repository.MeseGratisRiscattatoRepository;
import it.nutrizionista.restnutrizionista.repository.MessaggioAssistenzaRepository;
import it.nutrizionista.restnutrizionista.repository.OrariStudioRepository;
import it.nutrizionista.restnutrizionista.repository.PastoTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.PremioRiscattatoRepository;
import it.nutrizionista.restnutrizionista.repository.PresetObiettivoRepository;
import it.nutrizionista.restnutrizionista.repository.ProgressioneNutrizionistaRepository;
import it.nutrizionista.restnutrizionista.repository.PromemoriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.TicketAssistenzaRepository;
import it.nutrizionista.restnutrizionista.repository.UtentePreferitoRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/** Logica di business per Utenti. */
@Service
public class UtenteService {

    @Autowired private UtenteRepository repo;
    @Autowired private RuoloRepository ruoloRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private CredenzialeDemoRepository credenzialeDemoRepository;
    @Autowired private ClienteRepository clienteRepository; // F-USER-DEL: elenco clienti del nutrizionista
    @Autowired private ClienteService clienteService;        // F-USER-DEL: erasure canonica per-cliente
    // F-USER-DEL: repository dei figli utente-owned non-cascade (svuotati in eliminaFigliNonCascadeUtente)
    @Autowired private MessaggioAssistenzaRepository messaggioAssistenzaRepository;
    @Autowired private TicketAssistenzaRepository ticketAssistenzaRepository;
    @Autowired private UtentePreferitoRepository utentePreferitoRepository;
    @Autowired private PastoTemplateRepository pastoTemplateRepository;
    @Autowired private SchedaTemplateRepository schedaTemplateRepository;
    @Autowired private EventoGamificationRepository eventoGamificationRepository;
    @Autowired private BadgeSbloccatoRepository badgeSbloccatoRepository;
    @Autowired private ProgressioneNutrizionistaRepository progressioneNutrizionistaRepository;
    @Autowired private PremioRiscattatoRepository premioRiscattatoRepository;
    @Autowired private MeseGratisRiscattatoRepository meseGratisRiscattatoRepository;
    @Autowired private AppuntamentoRepository appuntamentoRepository;
    @Autowired private PromemoriaRepository promemoriaRepository;
    @Autowired private OrariStudioRepository orariStudioRepository;
    @Autowired private PresetObiettivoRepository presetObiettivoRepository;
    @Autowired private AttivitaRecenteRepository attivitaRecenteRepository;
    @Autowired private AccettazioneDocumentoRepository accettazioneDocumentoRepository;
    @Autowired private AlimentoBaseRepository alimentoBaseRepository;

    // I10 — erasure cross-store art.17: presente SOLO in keycloak-mode (bean gated in KeycloakAdminConfig).
    // In legacy l'ObjectProvider è vuoto → getIfAvailable()==null → nessuna chiamata (zero regressioni).
    @Autowired private ObjectProvider<KeycloakAdminClient> keycloakAdminClient;

    private static final Logger log = LoggerFactory.getLogger(UtenteService.class);

    /*lolo*/

    @Transactional
    public UtenteDto create(UtenteFormDto form) {
        Utente u = new Utente();
        apply(u, form, true);
        return DtoMapper.toUtenteDto(repo.save(u)); // Ruolo light dentro UtenteDto
    }

    /** Aggiorna utente. */
    @Transactional
    public UtenteDto update(UtenteFormDto form) {
        if (form.getId() == null) throw new BadRequestException("Id utente obbligatorio per update");
        Utente u = repo.findById(form.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
        apply(u, form, false);
        return DtoMapper.toUtenteDto(repo.save(u)); // Ruolo light
    }
    /** Aggiorna Profilo Utente. */
    @Transactional
    public UtenteDto updateProfile(UtenteFormDto form) {
        if (form.getId() == null) throw new BadRequestException("Id utente obbligatorio per update");
        Utente u = repo.findById(form.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
        apply(u, form, false);
        return DtoMapper.toUtenteDtoLight(repo.save(u)); // Senza Ruolo
    }

    /** Elimina utente (admin). Instrada la cancellazione dei clienti via il metodo canonico (F-USER-DEL). */
    @Transactional
    public void delete(Long id) {
        Utente u = repo.findById(id).orElseThrow(() -> new NotFoundException("Utente non trovato"));
        deleteAccount(u);
    }
    
    /** Cancellazione del proprio account (self-service). */
    @Transactional
    public void deleteMyProfile() {
        deleteAccount(currentUserService.getMe());
    }

    /**
     * Cancellazione account (F-USER-DEL): dipendenza UNIDIREZIONALE UtenteService → ClienteService.
     * Per ogni cliente del nutrizionista invoca la foglia canonica {@code eliminaClienteCompleto} (audit A7 +
     * PDF su disco + anonimizzazione gamification), poi cancella l'Utente. Il cascade ORM su {@code Utente.clienti}
     * è stato RIMOSSO apposta: questo loop è l'unica via di erasure dei clienti; il cascade residuo su
     * {@code Utente} tocca solo le collezioni Utente-owned (es. preferiti).
     */
    @Transactional
    public void deleteAccount(Utente u) {
        // I10 — cattura l'erasure key Keycloak PRIMA del delete (dopo, u è rimosso/detached).
        final String subjectId = u.getSubjectId();
        for (Cliente c : clienteRepository.findByNutrizionista_Id(u.getId())) {
            clienteService.eliminaClienteCompleto(c, AuditAction.DELETE, null);
        }
        eliminaFigliNonCascadeUtente(u.getId());
        repo.delete(u);
        scheduleKeycloakUserDeletion(subjectId);
    }

    /**
     * I10 — erasure CROSS-STORE (art. 17): dopo il COMMIT del delete DB, cancella l'identità dell'utente su
     * Keycloak (Admin API). <b>DB-first</b>: l'hook parte SOLO se la tx DB committa (modello
     * {@code FascicoloService.unlinkFilesDopoCommit}). <b>Gated keycloak-mode</b> (bean assente in legacy → no-op)
     * <b>+ {@code subjectId != null}</b> (utente mai loggato via IdP → nessuna identità da cancellare).
     * <b>Best-effort</b>: un errore KC NON fa fallire l'erasure (il DB è già pulito; il reconcile sweep — I10
     * Fase 2 — recupera l'eventuale identità orfana).
     */
    private void scheduleKeycloakUserDeletion(String subjectId) {
        KeycloakAdminClient client = keycloakAdminClient.getIfAvailable();
        if (client == null || subjectId == null) return; // legacy-mode oppure utente senza identità IdP
        Runnable kcDelete = () -> {
            try {
                client.deleteUser(subjectId);
            } catch (Exception e) {
                // Best-effort: l'erasure DB è già committata → l'obbligo art.17 sul nostro store è assolto.
                log.warn("I10: cancellazione identità Keycloak fallita (sub={}) — recupero al reconcile sweep. Causa: {}",
                        subjectId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { kcDelete.run(); }
            });
        } else {
            kcDelete.run(); // nessuna tx attiva (fallback, come FascicoloService) → esegui subito
        }
    }

    /**
     * Svuota i figli utente-owned che il cascade ORM su {@code Utente} NON tocca (mappa solo {@code preferiti}),
     * in ordine sicuro rispetto alle FK RESTRICT verso {@code utenti} (grafo verificato sul DB reale). Mirror
     * del tier cliente ({@code ClienteService.eliminaFigliNonCascade}). Le tabelle denormalizzate SENZA FK
     * (es. {@code audit_log}, {@code audit_account_demo}) NON compaiono qui di proposito (retention/prova).
     * <p>⚠️ <b>Regola di coverage (non negoziabile):</b> ogni nuova entità con FK verso {@code utenti} che NON
     * sia una collezione cascade di {@code Utente} va aggiunta QUI + seminata in
     * {@code UtenteDeleteAccountCascadeIntegrationTest}.
     */
    private void eliminaFigliNonCascadeUtente(Long utenteId) {
        // 1) Assistenza: i messaggi PRIMA dei ticket (FK ticket_id). deleteByTicket_Nutrizionista_Id svuota
        //    TUTTI i messaggi dei ticket del nutrizionista (anche le risposte del super admin).
        messaggioAssistenzaRepository.deleteByTicket_Nutrizionista_Id(utenteId);
        ticketAssistenzaRepository.deleteByNutrizionista_Id(utenteId);

        // 2) Preferiti + template PRIMA degli alimenti personali (li referenziano via FK non-null).
        utentePreferitoRepository.deleteByUtenteId(utenteId);
        // Template: ENTITY-delete (deleteAll su alberi full-tree) → cascade + orphanRemoval scendono i figli;
        // un bulk deleteBy cieco lascerebbe orfani sui figli del template. Il catalogo AlimentoBase resta intatto.
        pastoTemplateRepository.deleteAll(pastoTemplateRepository.findByCreatedByIdWithFullTree(utenteId));
        schedaTemplateRepository.deleteAll(schedaTemplateRepository.findByCreatedByIdWithFullTree(utenteId));

        // 3) Gamification (entità foglia, ordine interno libero).
        eventoGamificationRepository.deleteByNutrizionista_Id(utenteId);
        badgeSbloccatoRepository.deleteByNutrizionista_Id(utenteId);
        progressioneNutrizionistaRepository.deleteByNutrizionista_Id(utenteId);
        premioRiscattatoRepository.deleteByNutrizionista_Id(utenteId);
        meseGratisRiscattatoRepository.deleteByNutrizionista_Id(utenteId);

        // 4) Calendario/studio/preset/attività/accettazioni/demo.
        appuntamentoRepository.deleteByNutrizionista_Id(utenteId);   // i manuali senza cliente (i cliente-linked già rimossi)
        promemoriaRepository.deleteByNutrizionista_Id(utenteId);
        orariStudioRepository.deleteByNutrizionista_Id(utenteId);
        presetObiettivoRepository.deleteByNutrizionista_Id(utenteId);
        attivitaRecenteRepository.deleteByNutrizionista_Id(utenteId); // residui non-cliente
        accettazioneDocumentoRepository.deleteByUtente_Id(utenteId);
        credenzialeDemoRepository.deleteByUtente_Id(utenteId);

        // 5) ULTIMO: alimenti PERSONALI del nutrizionista (created_by=utenteId). I GLOBALI (created_by NULL)
        //    restano INTATTI (esclusi per costruzione dalla WHERE del derived). Sicuro perché clienti + template
        //    + preferiti che li referenziavano sono già stati rimossi sopra.
        alimentoBaseRepository.deleteByCreatedBy_Id(utenteId);
    }
    
    /** Dettaglio utente. (Ruolo light) */
    @Transactional(readOnly = true)
    public UtenteDto getById(Long id) {
        return repo.findById(id)
                .map(DtoMapper::toUtenteDto)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
    }

    /** Lista paginata utenti (mapper light). */
    @Transactional(readOnly = true)
    public PageResponse<UtenteDto> list(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toUtenteDtoLight));
    }

    /** Lista completa non paginata (mapper light). */
    @Transactional(readOnly = true)
    public List<UtenteDto> listAll() {
        return repo.findAll().stream()
                .map(DtoMapper::toUtenteDtoLight)
                .collect(Collectors.toList());
    }

    /** Profilo utente corrente (ricavato dal JWT). */
    @Transactional(readOnly = true)
    public UtenteDto getProfile() {
        Utente u = currentUserService.getMe();
        return DtoMapper.toUtenteDto(u);
    }

    /** Profilo leggero per navbar/home: solo id, nome, cognome, logo. */
    @Transactional(readOnly = true)
    public ProfiloLightDto getProfileLight() {
        Utente u = currentUserService.getMe();
        return new ProfiloLightDto(
                u.getId(),
                u.getNome(),
                u.getCognome(),
                u.getFilePathLogo(),
                u.getRuolo() != null ? u.getRuolo().getNome() : null,
                u.getEmail()
        );
    }
    /** Copia campi dal form, gestendo password e ruolo. */
    private void apply(Utente u, UtenteFormDto form, boolean encodePasswordAlways) {
        u.setNome(form.getNome());
        u.setCognome(form.getCognome());
        u.setCodiceFiscale(form.getCodiceFiscale());
        u.setEmail(form.getEmail());
        if (encodePasswordAlways || (form.getPassword() != null && !form.getPassword().isBlank())) {
            u.setPassword(encoder.encode(form.getPassword()));
        }
        u.setDataNascita(form.getDataNascita());
        u.setTelefono(form.getTelefono());
        u.setIndirizzo(form.getIndirizzo());

        Ruolo ruolo = ruoloRepo.findById(form.getRuolo().getId())
                .orElseThrow(() -> new NotFoundException("Ruolo non trovato"));
        u.setRuolo(ruolo);
    }
    
    public UtenteDto updateMyProfile(UtenteProfileUpdateDto form) {
        Utente u = currentUserService.getMe();

        u.setNome(form.getNome());
        u.setCognome(form.getCognome());
        u.setCodiceFiscale(form.getCodiceFiscale());
        u.setDataNascita(form.getDataNascita());
        u.setTelefono(form.getTelefono());
        u.setIndirizzo(form.getIndirizzo());

        return DtoMapper.toUtenteDto(repo.save(u));
    }

    
    public UtenteDto updateMyPassword(ResetPasswordDto dto) {    	
        Utente utente = currentUserService.getMe();
        if (credenzialeDemoRepository.existsByUtente_Id(utente.getId())) {
            throw new BadRequestException(
                    "La password degli account demo puo essere gestita solo dal super admin");
        }
       
        if (!dto.getPassword().equals(dto.getConfermaPassword())) {
            throw new BadRequestException("Le password inserite non coincidono");
        }
       
        utente.setPassword(encoder.encode(dto.getPassword()));     
        Utente updated = repo.save(utente);
        return DtoMapper.toUtenteDto(updated);
    }
    
    public UtenteDto updateLogo (LogoRequestDto logo) throws IOException {
        Utente utente = currentUserService.getMe();
        
        if (utente.getId() != logo.getUtenteId()) throw new BadRequestException("L'utente non corrisponde");
        
        MultipartFile file = logo.getImage();
	    if (file != null && !file.isEmpty()) {

	        String contentType = file.getContentType();
	        if (contentType == null || !contentType.startsWith("image/")) {
	            throw new IllegalArgumentException("Il file caricato non è un'immagine valida (MIME non corretto)");
	        }
	        List<String> allowedExtensions = List.of("jpg", "jpeg", "png");

	        String originalFilename = file.getOriginalFilename();
	        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

	        if (!allowedExtensions.contains(extension)) {
	            throw new IllegalArgumentException("Estensione non valida: " + extension);
	        }

	        // 🔹 Salvataggio fisico
	        Path uploadDir = Paths.get("uploads/loghi");
	        if (!Files.exists(uploadDir)) {
	            Files.createDirectories(uploadDir);
	        }
	        
	        String cleanName = originalFilename.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
	        String fileName = System.currentTimeMillis() + "_" + cleanName;
	        Path filePath = uploadDir.resolve(fileName);

	        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

	        // 🔹 Salva percorso relativo
	        utente.setFilePathLogo("uploads/loghi/" + fileName);
	    }
		return DtoMapper.toUtenteDto(repo.save(utente));
    }
}
