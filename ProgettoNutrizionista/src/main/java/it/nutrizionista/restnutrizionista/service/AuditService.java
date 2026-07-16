package it.nutrizionista.restnutrizionista.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import it.nutrizionista.restnutrizionista.dto.AuditLogDto;
import it.nutrizionista.restnutrizionista.dto.ConflittoClinicoDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;

/**
 * Servizio di audit GDPR (A7): registra gli accessi ai dati sanitari nell'entità immutabile
 * {@link AuditLog}. Popolamento <b>esplicito</b> dai service di dominio (niente AOP).
 *
 * <p>Principio: il contesto (attore, IP, user-agent) è catturato <b>sul thread sincrono</b>
 * — {@code SecurityContext}/{@code RequestContext} non si propagano al thread {@code @Async}.
 *
 * <p>Tre modalità di scrittura, con propagazione transazionale diversa:
 * <ul>
 *   <li>{@link #record} — letture/liste: <b>async</b> fire-and-forget, non rilancia mai.</li>
 *   <li>{@link #recordCriticalNewTx} — eventi in uscita (SHARE/EXPORT_PDF/DOWNLOAD): <b>sincrono in
 *       tx propria</b> ({@code REQUIRES_NEW}), da chiamare <b>prima</b> del side-effect. Un fallimento
 *       del save risale → l'azione critica si annulla ("no log ⇒ no disclosure").</li>
 *   <li>{@link #recordCriticalSameTx} — DELETE: <b>stessa tx</b> del delete (la riga committa iff il delete committa).</li>
 *   <li>{@link #recordDenied} — accessi negati: <b>sincrono, {@code REQUIRES_NEW}</b> (sopravvive al
 *       rollback della {@code ForbiddenException}).</li>
 * </ul>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository repo;

    @Autowired
    private CurrentUserService currentUserService;

    /** Self-injection: per invocare {@link #persistAsync} attraverso il proxy (no self-invocation). */
    @Autowired
    @Lazy
    private AuditService self;

    /** Contesto catturato sul thread sincrono (non propagato all'async). Package-private per il proxy CGLIB. */
    record AuditContext(Long utenteId, String utenteEmail, String ipAddress, String userAgent) {
    }

    AuditContext captureContext() {
        Long utenteId = null;
        String email = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                email = auth.getName();
            }
            utenteId = currentUserService.getMe().getId();
        } catch (Exception ignore) {
            // Attore non risolvibile (raro): si registra comunque l'evento con i campi disponibili.
        }
        String ip = null;
        String ua = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            ip = sra.getRequest().getRemoteAddr();
            ua = sra.getRequest().getHeader("User-Agent");
        }
        return new AuditContext(utenteId, email, ip, ua);
    }

    // ---------- Letture / liste: ASYNC, fire-and-forget, mai rilancia ----------

    public void record(AuditAction action, AuditEntityType entityType, Long entityId, Long clienteId) {
        try {
            AuditContext ctx = captureContext();
            self.persistAsync(ctx, action, entityType, entityId, clienteId, null, AuditOutcome.SUCCESS);
        } catch (Exception e) {
            log.error("Audit record fallito (action={}, entityType={}, id={})", action, entityType, entityId, e);
        }
    }

    @Async("auditExecutor")
    public void persistAsync(AuditContext ctx, AuditAction action, AuditEntityType entityType,
                             Long entityId, Long clienteId, String destinatario, AuditOutcome esito) {
        try {
            save(ctx, action, entityType, entityId, clienteId, destinatario, esito);
        } catch (Exception e) {
            log.error("Audit persistAsync fallito (action={}, entityType={}, id={})", action, entityType, entityId, e);
        }
    }

    // ---------- Eventi critici in uscita: SINCRONI in tx propria, PRIMA del side-effect ----------

    /**
     * Registra un evento critico (SHARE/EXPORT_PDF/DOWNLOAD) in una transazione PROPRIA committata
     * subito. Chiamare PRIMA del side-effect: se il save fallisce l'eccezione risale e il side-effect
     * non viene eseguito. Nessun try/catch di soppressione: è intenzionale (no log ⇒ no disclosure).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCriticalNewTx(AuditAction action, AuditEntityType entityType, Long entityId,
                                    Long clienteId, String destinatario, AuditOutcome esito) {
        save(captureContext(), action, entityType, entityId, clienteId, destinatario, esito);
    }

    /**
     * Registra il DELETE nella STESSA transazione del chiamante: la riga di audit committa se e solo
     * se il delete committa (nessun "cancellato ma non registrato").
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordCriticalSameTx(AuditAction action, AuditEntityType entityType, Long entityId, Long clienteId) {
        save(captureContext(), action, entityType, entityId, clienteId, null, AuditOutcome.SUCCESS);
    }

    /**
     * Registra un override consapevole di un blocco clinico grave ({@code OVERRIDE_ALERT_GRAVE}) nella
     * STESSA transazione del chiamante: la riga di audit committa se e solo se l'operazione forzata
     * (l'inserimento dell'alimento) committa. Così non esistono override "phantom" (loggati ma non
     * avvenuti) né inserimenti forzati senza traccia. I motivi del blocco vanno in {@code dettaglio}.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordOverrideSameTx(AuditEntityType entityType, Long entityId, Long clienteId, String dettaglio) {
        save(captureContext(), AuditAction.OVERRIDE_ALERT_GRAVE, entityType, entityId, clienteId, null,
                AuditOutcome.SUCCESS, dettaglio);
    }

    /**
     * Variante BATCH dell'override consapevole (finding F-D1a): registra UNA riga
     * {@code OVERRIDE_ALERT_GRAVE} per ciascun alimento grave incluso consapevolmente applicando un
     * template o duplicando una scheda cross-paziente. Stessa transazione del chiamante (le righe
     * committano iff l'operazione forzata committa). Il contesto (attore/IP/UA) è catturato una sola
     * volta per l'intero batch. {@code entityType=SCHEDA}, {@code entityId=schedaId}; l'alimento e i
     * motivi finiscono nel {@code dettaglio} (troncato a 1024).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordOverrideGraviSameTx(Long schedaId, Long clienteId,
                                          List<ConflittoClinicoDto> inclusi, String contesto) {
        if (inclusi == null || inclusi.isEmpty()) {
            return;
        }
        AuditContext ctx = captureContext();
        for (ConflittoClinicoDto c : inclusi) {
            String dettaglio = "Override " + contesto + " — alimento '" + c.nome() + "' (id=" + c.alimentoId()
                    + ") scheda id=" + schedaId + ". Motivi: " + c.motivi();
            if (dettaglio.length() > 1024) {
                dettaglio = dettaglio.substring(0, 1024);
            }
            save(ctx, AuditAction.OVERRIDE_ALERT_GRAVE, AuditEntityType.SCHEDA, schedaId, clienteId, null,
                    AuditOutcome.SUCCESS, dettaglio);
        }
    }

    /**
     * Registra un accesso NEGATO (ownership fallita). {@code action=ACCESS} (intento ignoto),
     * {@code esito=DENIED}. Sincrono e in {@code REQUIRES_NEW} così la riga sopravvive al rollback
     * provocato dalla {@code ForbiddenException} sulla transazione del chiamante.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(AuditEntityType entityType, Long entityId) {
        save(captureContext(), AuditAction.ACCESS, entityType, entityId, null, null, AuditOutcome.DENIED);
    }

    // ---------- Consultazione "storico accessi" (A7) ----------

    /** Vista globale (titolare/admin): filtri opzionali su cliente/utente/azione/intervallo. */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> search(Long clienteId, Long utenteId, AuditAction action,
                                            Instant from, Instant to, Pageable pageable) {
        return PageResponse.from(repo.search(clienteId, utenteId, action, from, to, pageable)
                .map(DtoMapper::toAuditLogDto));
    }

    /**
     * Self-audit del nutrizionista: forza {@code utenteId = utente corrente} lato server (mai fidarsi
     * di un parametro client) → vede SOLO i propri accessi.
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> searchOwn(Long clienteId, AuditAction action,
                                               Instant from, Instant to, Pageable pageable) {
        Long me = currentUserService.getMe().getId();
        return PageResponse.from(repo.search(clienteId, me, action, from, to, pageable)
                .map(DtoMapper::toAuditLogDto));
    }

    private void save(AuditContext ctx, AuditAction action, AuditEntityType entityType, Long entityId,
                      Long clienteId, String destinatario, AuditOutcome esito) {
        save(ctx, action, entityType, entityId, clienteId, destinatario, esito, null);
    }

    private void save(AuditContext ctx, AuditAction action, AuditEntityType entityType, Long entityId,
                      Long clienteId, String destinatario, AuditOutcome esito, String dettaglio) {
        repo.save(new AuditLog(ctx.utenteId(), ctx.utenteEmail(), action, entityType, entityId,
                clienteId, esito, ctx.ipAddress(), ctx.userAgent(), destinatario, dettaglio));
    }
}
