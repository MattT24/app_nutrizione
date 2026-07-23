package it.nutrizionista.restnutrizionista.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.RetentionReport;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;

/**
 * A6 — Retention / storage limitation (art. 5(1)(e) + art. 17 GDPR). Ciclo notturno che, allo scadere della
 * conservazione dei dati clinici, mette in <b>quarantena</b> i clienti inattivi e, dopo la grace, li <b>purga</b>
 * via il metodo canonico {@link ClienteService#eliminaClienteCompleto}. Operazione irreversibile a massimo raggio
 * → safety non negoziabile, direzione d'errore SEMPRE verso l'over-retention (mai cancellare un cliente attivo).
 *
 * <p>Valvole: <b>dry-run</b> (default sicuro: solo log), <b>quarantena+grace</b>, <b>breaker catastrofico</b>
 * (gatea solo mark+purge, non il clear), <b>rate-limit</b> per-run (backlog loggato), <b>re-check TOCTOU</b> in-tx
 * sotto lock prima del purge, <b>lock singola-esecuzione</b>. Le durate sono PROVVISORIE (da confermare col
 * legale) e vivono in config. Non {@code @Transactional} a livello di ciclo: orchestra azioni per-cliente
 * transazionali (via self-proxy {@code @Lazy}) così un fallimento isolato non aborta l'intero ciclo.
 */
@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final AuditService auditService;
    private final RetentionService self; // self-proxy per applicare @Transactional ai metodi per-cliente

    @Value("${retention.clinical.years:10}") private int clinicalYears;
    @Value("${retention.quarantine.days:90}") private int quarantineDays;
    @Value("${retention.dry-run:true}") private boolean dryRun;
    @Value("${retention.safety-threshold-percent:20}") private int safetyPercent;
    @Value("${retention.safety-threshold-min-absolute:10}") private int safetyMinAbs;
    @Value("${retention.max-purge-per-run:50}") private int maxPurgePerRun;

    /** Lock esecuzione singola (#10): niente cicli concorrenti (ShedLock = nota per multi-istanza futura). */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RetentionService(ClienteRepository clienteRepository, ClienteService clienteService,
                            AuditService auditService, @Lazy RetentionService self) {
        this.clienteRepository = clienteRepository;
        this.clienteService = clienteService;
        this.auditService = auditService;
        this.self = self;
    }

    /** Esegue un ciclo di retention. Ritorna il {@link RetentionReport} (null se un ciclo è già in corso). */
    public RetentionReport eseguiCicloRetention() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Retention: ciclo già in corso, skip.");
            return null;
        }
        try {
            return eseguiInterno();
        } finally {
            running.set(false); // R3: SEMPRE (anche su eccezione), altrimenti il job non riparte mai più.
        }
    }

    private RetentionReport eseguiInterno() {
        // Timezone-safe, coerente con hibernate.jdbc.time_zone=UTC.
        LocalDate cutoffDate = LocalDate.now(ZoneOffset.UTC).minusYears(clinicalYears);
        Instant cutoff = cutoffDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant graceCutoff = Instant.now().minus(Duration.ofDays(quarantineDays));

        long total = clienteRepository.count();
        List<Cliente> inattivi = clienteRepository.findInattiviPerRetention(cutoff, cutoffDate);

        // Breaker catastrofico (#6/Q2): scatta se troppi eleggibili in un colpo. Gatea SOLO mark+purge.
        boolean breaker = inattivi.size() > (total * safetyPercent / 100) && inattivi.size() > safetyMinAbs;

        List<Cliente> inHold = inattivi.stream().filter(this::isUnderLegalHold).toList();
        List<Cliente> candidati = inattivi.stream().filter(c -> !isUnderLegalHold(c)).toList();
        List<Long> idInHold = inHold.stream().map(Cliente::getId).toList();

        int pianificatiQ = (int) candidati.stream().filter(c -> c.getDataQuarantena() == null).count();
        int pianificatiP = (int) candidati.stream()
                .filter(c -> c.getDataQuarantena() != null && c.getDataQuarantena().isBefore(graceCutoff)).count();

        // ── DRY-RUN: solo diagnostica, ZERO scritture (governance #8: WARN ad ogni run finché attivo). ──
        if (dryRun) {
            log.warn("RETENTION IN DRY-RUN — nessuna cancellazione reale; attivare dopo review. "
                    + "total={}, inattivi={}, inHold={}, would-quarantena={}, would-purge={}, breaker={}",
                    total, inattivi.size(), inHold.size(), pianificatiQ, pianificatiP, breaker);
            return new RetentionReport(true, breaker, total, inattivi.size(), inHold.size(),
                    pianificatiQ, pianificatiP, 0, 0, 0, 0, 0, idInHold, List.of());
        }

        int falliti = 0;

        // ── clear stale quarantena (#5) — gira SEMPRE, anche a breaker scattato (R1). Sorgente indipendente
        //    dagli "inattivi" del run: previene il marker congelato che salterebbe la grace. ──
        for (Cliente q : clienteRepository.findByDataQuarantenaIsNotNull()) {
            Long id = q.getId();
            try {
                boolean nonPiuInattivo = clienteRepository.findInattivoById(id, cutoff, cutoffDate).isEmpty();
                if (nonPiuInattivo || isUnderLegalHold(q)) {
                    self.clearQuarantena(id);
                }
            } catch (Exception e) {
                falliti++;
                log.error("Retention: clearQuarantena fallito per cliente {}", id, e);
            }
        }

        if (breaker) {
            log.error("RETENTION BREAKER SCATTATO — inattivi={} su total={} (soglia {}% / floor {}). "
                    + "mark+purge SALTATI; solo il clear protettivo è stato eseguito. Verifica manuale richiesta.",
                    inattivi.size(), total, safetyPercent, safetyMinAbs);
            return new RetentionReport(false, true, total, inattivi.size(), inHold.size(),
                    pianificatiQ, pianificatiP, 0, 0, 0, 0, falliti, idInHold, List.of());
        }

        // ── mark: quarantena dei candidati non ancora marcati. ──
        int eseguitiQ = 0;
        for (Cliente c : candidati) {
            if (c.getDataQuarantena() != null) continue;
            Long id = c.getId();
            try {
                self.marcaQuarantena(id, "Retention art.5(1)(e): inattivo da " + clinicalYears + " anni");
                eseguitiQ++;
            } catch (Exception e) {
                falliti++;
                log.error("Retention: marcaQuarantena fallito per cliente {}", id, e);
            }
        }

        // ── purge (rate-limited, oldest-first R4): solo chi ha superato la grace. ──
        List<Cliente> daPurgare = candidati.stream()
                .filter(c -> c.getDataQuarantena() != null && c.getDataQuarantena().isBefore(graceCutoff))
                .sorted(Comparator.comparing(Cliente::getDataQuarantena))
                .toList();
        int backlog = Math.max(0, daPurgare.size() - maxPurgePerRun);
        int eseguitiP = 0;
        int saltatiToctou = 0;
        List<Long> idPurge = new ArrayList<>();
        for (Cliente c : daPurgare.stream().limit(maxPurgePerRun).toList()) {
            Long id = c.getId();
            try {
                if (self.purgaCliente(id, cutoff, cutoffDate, graceCutoff)) {
                    eseguitiP++;
                    idPurge.add(id);
                } else {
                    saltatiToctou++;
                }
            } catch (Exception e) {
                falliti++;
                log.error("Retention: purge fallito per cliente {} (gli altri proseguono)", id, e);
            }
        }
        if (backlog > 0) {
            log.warn("Retention: {} clienti purgabili oltre il rate-limit ({}/run) → rimandati al prossimo ciclo (nessun troncamento silenzioso).",
                    backlog, maxPurgePerRun);
        }

        RetentionReport report = new RetentionReport(false, false, total, inattivi.size(), inHold.size(),
                pianificatiQ, pianificatiP, eseguitiQ, eseguitiP, saltatiToctou, backlog, falliti, idInHold, idPurge);
        log.info("Retention ciclo reale: {}", report);
        return report;
    }

    /** Marca la quarantena (set {@code dataQuarantena}) + audit {@code RETENTION_QUARANTENA} nella STESSA tx.
     *  NON tocca {@code ultimoContattoClinico} (nessuna oscillazione del segnale clinico). */
    @Transactional
    public void marcaQuarantena(Long id, String dettaglio) {
        Cliente c = clienteRepository.findById(id).orElse(null);
        if (c == null) return; // sparito → no-op (non contato come fallito)
        c.setDataQuarantena(Instant.now());
        auditService.recordCriticalSameTx(AuditAction.RETENTION_QUARANTENA, AuditEntityType.CLIENTE, id, id, dettaglio);
        clienteRepository.save(c);
    }

    /** Azzera la quarantena (recovery/hold). NON tocca {@code ultimoContattoClinico}. */
    @Transactional
    public void clearQuarantena(Long id) {
        Cliente c = clienteRepository.findById(id).orElse(null);
        if (c == null) return;
        c.setDataQuarantena(null);
        clienteRepository.save(c);
    }

    /**
     * Purga con re-verifica TOCTOU (#1) in-tx sotto lock riga. Ritorna {@code true} se cancellato, {@code false}
     * se saltato (stato cambiato dallo snapshot: attività riapparsa / quarantena azzerata / entrato in hold).
     */
    @Transactional
    public boolean purgaCliente(Long id, Instant cutoff, LocalDate cutoffDate, Instant graceCutoff) {
        Cliente locked = clienteRepository.findByIdConLock(id).orElse(null);
        if (locked == null) return false; // assente → no-op
        // Ri-controllo con lo STESSO predicato batch (parametrizzato) → niente deriva tra snapshot e purge.
        Cliente c = clienteRepository.findInattivoById(id, cutoff, cutoffDate).orElse(null);
        if (c == null
                || c.getDataQuarantena() == null
                || !c.getDataQuarantena().isBefore(graceCutoff)
                || isUnderLegalHold(c)) {
            log.info("Retention: purge SALTATO per cliente {} (TOCTOU: stato cambiato dallo snapshot).", id);
            return false;
        }
        clienteService.eliminaClienteCompleto(c, AuditAction.RETENTION_PURGE,
                "Retention art.5(1)(e)/art.17: purge dopo quarantena (inattivo da " + clinicalYears + " anni)");
        return true;
    }

    /** Legal hold (#9): art. 17(3) — contenzioso/obbligo legale (oltre l'art. 18) escludono dal purge. */
    private boolean isUnderLegalHold(Cliente c) {
        return c.isTrattamentoLimitato() || c.isLegalHold();
    }
}
