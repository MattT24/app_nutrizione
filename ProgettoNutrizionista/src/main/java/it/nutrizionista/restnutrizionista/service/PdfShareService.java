package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.ShareRequest;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invio via email dei PDF generati (scheda, misurazione, plicometria) con destinatario
 * vincolato all'email registrata del cliente (QW-4 / E4b). Ownership sempre verificata via
 * {@link OwnershipValidator}. {@code @Transactional} perché {@code cliente} è LAZY e va letto
 * entro la sessione (open-in-view=false).
 */
@Service
public class PdfShareService {

    private final OwnershipValidator ownershipValidator;
    private final LimitazioneTrattamentoValidator limitazioneValidator;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final AuditService auditService;

    public PdfShareService(OwnershipValidator ownershipValidator, LimitazioneTrattamentoValidator limitazioneValidator,
                           PdfService pdfService, EmailService emailService, AuditService auditService) {
        this.ownershipValidator = ownershipValidator;
        this.limitazioneValidator = limitazioneValidator;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public void shareScheda(Long id, ShareRequest req) {
        Scheda scheda = ownershipValidator.getOwnedScheda(id);
        limitazioneValidator.assertNonLimitato(scheda.getCliente()); // A5.3: nessun invio a terzi per cliente limitato
        Long clienteId = clienteIdDi(scheda.getCliente());
        String to = ShareRecipient.resolve(emailDi(scheda.getCliente()), req);
        byte[] pdf = pdfService.generaPdfScheda(id);
        inviaConAudit(AuditEntityType.SCHEDA, id, clienteId, to, "La tua Scheda Nutrizionale",
                "In allegato trovi la tua scheda nutrizionale in formato PDF.", pdf, "scheda_" + id + ".pdf");
    }

    @Transactional(readOnly = true)
    public void shareMisurazione(Long id, ShareRequest req) {
        MisurazioneAntropometrica m = ownershipValidator.getOwnedMisurazioneAntropometrica(id);
        limitazioneValidator.assertNonLimitato(m.getCliente()); // A5.3
        Long clienteId = clienteIdDi(m.getCliente());
        String to = ShareRecipient.resolve(emailDi(m.getCliente()), req);
        byte[] pdf = pdfService.generaPdfMisurazione(id);
        inviaConAudit(AuditEntityType.MISURAZIONE_ANTROPOMETRICA, id, clienteId, to,
                "Il tuo report di Misurazione Antropometrica",
                "In allegato trovi il tuo report di misurazione antropometrica in formato PDF.", pdf, "misurazione_" + id + ".pdf");
    }

    @Transactional(readOnly = true)
    public void sharePlicometria(Long id, ShareRequest req) {
        Plicometria p = ownershipValidator.getOwnedPlicometria(id);
        limitazioneValidator.assertNonLimitato(p.getCliente()); // A5.3
        Long clienteId = clienteIdDi(p.getCliente());
        String to = ShareRecipient.resolve(emailDi(p.getCliente()), req);
        byte[] pdf = pdfService.generaPdfPlicometria(id);
        inviaConAudit(AuditEntityType.PLICOMETRIA, id, clienteId, to, "Il tuo report di Plicometria",
                "In allegato trovi il tuo report di plicometria in formato PDF.", pdf, "plicometria_" + id + ".pdf");
    }

    /**
     * Registra l'evento SHARE (audit critico A7) in tx propria <b>prima</b> di inviare — se il save
     * fallisce l'invio non avviene ("no log ⇒ no disclosure") — poi invia in modo sincrono; su invio
     * fallito registra una riga {@code FAILURE} e rilancia.
     */
    private void inviaConAudit(AuditEntityType tipo, Long id, Long clienteId, String to,
                               String subject, String body, byte[] pdf, String filename) {
        auditService.recordCriticalNewTx(AuditAction.SHARE, tipo, id, clienteId, to, AuditOutcome.SUCCESS);
        try {
            emailService.sendPdfEmail(to, subject, body, pdf, filename);
        } catch (RuntimeException e) {
            auditService.recordCriticalNewTx(AuditAction.SHARE, tipo, id, clienteId, to, AuditOutcome.FAILURE);
            throw e;
        }
    }

    private String emailDi(Cliente cliente) {
        return cliente == null ? null : cliente.getEmail();
    }

    private Long clienteIdDi(Cliente cliente) {
        return cliente == null ? null : cliente.getId();
    }
}
