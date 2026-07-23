package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.DocumentoFascicoloDto;
import it.nutrizionista.restnutrizionista.dto.SalvaDocumentoRequest;
import it.nutrizionista.restnutrizionista.dto.ShareRequest;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FascicoloService {

    private final DocumentoFascicoloRepository fascicoloRepository;
    private final PdfService pdfService;
    private final OwnershipValidator ownershipValidator;
    private final LimitazioneTrattamentoValidator limitazioneValidator;
    private final EmailService emailService;
    private final AuditService auditService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FascicoloService.class);

    private final String uploadDir = "uploads/fascicoli";

    public FascicoloService(DocumentoFascicoloRepository fascicoloRepository, PdfService pdfService,
                            OwnershipValidator ownershipValidator, LimitazioneTrattamentoValidator limitazioneValidator,
                            EmailService emailService, AuditService auditService) {
        this.fascicoloRepository = fascicoloRepository;
        this.pdfService = pdfService;
        this.ownershipValidator = ownershipValidator;
        this.limitazioneValidator = limitazioneValidator;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    public DocumentoFascicoloDto salvaDocumento(SalvaDocumentoRequest request) {
        // F-OWN-SWEEP: valida la proprietà del cliente PRIMA del dedup/early-return, altrimenti il
        // ramo "esistente" esporrebbe il DTO di un documento di un altro tenant (info-disclosure).
        Cliente cliente = ownershipValidator.getOwnedCliente(request.getClienteId());

        // Controllo se esiste già (solo dopo aver verificato l'ownership del cliente)
        var esistente = fascicoloRepository.findByClienteIdAndTipoDocumentoAndRiferimentoId(
                request.getClienteId(), request.getTipoDocumento(), request.getRiferimentoId());
        if (esistente.isPresent()) {
            return toDto(esistente.get()); // già archiviato: lettura del titolare, consentita anche se limitato
        }

        // A5.3: la CREAZIONE di un nuovo documento (produzione+persistenza) è bloccata se il cliente è limitato.
        limitazioneValidator.assertNonLimitato(cliente);

        byte[] pdfBytes;
        String titoloBase = "";

        switch (request.getTipoDocumento()) {
            case SCHEDA:
                Scheda scheda = ownershipValidator.getOwnedScheda(request.getRiferimentoId());
                pdfBytes = pdfService.generaPdfScheda(scheda.getId());
                titoloBase = "Scheda " + scheda.getNome();
                break;
            case MISURAZIONE:
                MisurazioneAntropometrica misurazione = ownershipValidator.getOwnedMisurazioneAntropometrica(request.getRiferimentoId());
                pdfBytes = pdfService.generaPdfMisurazione(misurazione.getId());
                titoloBase = "Misurazione del " + misurazione.getDataMisurazione().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                break;
            case PLICOMETRIA:
                Plicometria plicometria = ownershipValidator.getOwnedPlicometria(request.getRiferimentoId());
                pdfBytes = pdfService.generaPdfPlicometria(plicometria.getId());
                titoloBase = "Plicometria del " + plicometria.getDataMisurazione().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                break;
            default:
                throw new IllegalArgumentException("Tipo documento non supportato");
        }

        // Salva su disco
        try {
            Path clientDir = Paths.get(uploadDir, cliente.getId().toString());
            if (!Files.exists(clientDir)) {
                Files.createDirectories(clientDir);
            }

            String filename = UUID.randomUUID().toString() + ".pdf";
            Path filepath = clientDir.resolve(filename);
            Files.write(filepath, pdfBytes);

            DocumentoFascicolo doc = new DocumentoFascicolo();
            doc.setCliente(cliente);
            doc.setTitolo(titoloBase);
            doc.setTipoDocumento(request.getTipoDocumento());
            doc.setRiferimentoId(request.getRiferimentoId());
            doc.setPercorsoFile(filepath.toString());
            
            doc = fascicoloRepository.save(doc);
            return toDto(doc);

        } catch (IOException e) {
            throw new RuntimeException("Errore durante il salvataggio fisico del documento", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoFascicoloDto> getDocumentiByCliente(Long clienteId) {
        ownershipValidator.getOwnedCliente(clienteId); // difesa in profondità: il cliente dev'essere del nutrizionista corrente
        auditService.record(AuditAction.LIST, AuditEntityType.DOCUMENTO_FASCICOLO, null, clienteId);
        return fascicoloRepository.findByClienteIdOrderByDataCreazioneDesc(clienteId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocumento(Long id) {
        DocumentoFascicolo doc = ownershipValidator.getOwnedDocumentoFascicolo(id);
        Long clienteId = doc.getCliente() == null ? null : doc.getCliente().getId();
        // Audit critico (A7): registra il DOWNLOAD in tx propria PRIMA di restituire i byte.
        auditService.recordCriticalNewTx(AuditAction.DOWNLOAD, AuditEntityType.DOCUMENTO_FASCICOLO,
                id, clienteId, null, AuditOutcome.SUCCESS);
        try {
            return leggiBytesDocumento(doc);
        } catch (RuntimeException e) {
            // A7: side-effect fallito (lettura file) → riga FAILURE (il SUCCESS è già committato in REQUIRES_NEW), poi rilancio
            auditService.recordCriticalNewTx(AuditAction.DOWNLOAD, AuditEntityType.DOCUMENTO_FASCICOLO,
                    id, clienteId, null, AuditOutcome.FAILURE);
            throw e;
        }
    }

    /**
     * Lettura dei byte del PDF dal disco. Condivisa da {@link #downloadDocumento} (audita DOWNLOAD)
     * e {@link #shareDocumento} (audita SHARE) senza generare un doppio evento di audit.
     */
    private byte[] leggiBytesDocumento(DocumentoFascicolo doc) {
        try {
            return Files.readAllBytes(Paths.get(doc.getPercorsoFile()));
        } catch (IOException e) {
            throw new RuntimeException("Errore durante la lettura del file dal disco", e);
        }
    }
    
    @Transactional(readOnly = true)
    public DocumentoFascicolo getDocumentoEntity(Long id) {
        return ownershipValidator.getOwnedDocumentoFascicolo(id);
    }

    public void eliminaDocumento(Long id) {
        DocumentoFascicolo doc = ownershipValidator.getOwnedDocumentoFascicolo(id);
        limitazioneValidator.assertNonLimitato(doc.getCliente()); // A5.3: no delete del dato conservato se limitato
        String percorso = doc.getPercorsoFile();
        fascicoloRepository.delete(doc);
        // A6/MF3: prima il record, poi l'unlink fisico DOPO il commit (rollback ⇒ niente ghost-record: record vivo, PDF sparito).
        if (percorso != null) unlinkFilesDopoCommit(List.of(percorso));
    }

    /**
     * Rimuove TUTTI i documenti di fascicolo di un cliente (record + file su disco). Usato dalla
     * cancellazione del cliente (A5.1): senza questo, i documenti sanitari resterebbero orfani
     * (FK {@code cliente_id} NOT NULL) e i PDF resterebbero su disco. L'ownership del cliente è già
     * verificata dal chiamante e i documenti sono filtrati per {@code clienteId}, quindi non serve
     * un ulteriore check per singolo documento.
     */
    public void eliminaDocumentiDiCliente(Long clienteId) {
        List<DocumentoFascicolo> documenti = fascicoloRepository.findByClienteIdOrderByDataCreazioneDesc(clienteId);
        if (documenti.isEmpty()) return;
        List<String> percorsi = documenti.stream().map(DocumentoFascicolo::getPercorsoFile).collect(Collectors.toList());
        fascicoloRepository.deleteAll(documenti);
        unlinkFilesDopoCommit(percorsi); // A6 #2: unlink fisico solo DOPO il commit (rollback ⇒ i file restano)
    }

    /**
     * Unlink fisico dei PDF DOPO il commit della transazione (A6 #2): se la tx rolla back non si crea un
     * "file fantasma" (record vivo, file cancellato). Se non c'è una tx attiva (contesto non transazionale)
     * si cancella subito. Best-effort sul filesystem: un errore di unlink non deve far fallire la delete.
     */
    private void unlinkFilesDopoCommit(List<String> percorsi) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    percorsi.forEach(FascicoloService::deleteFileQuiet);
                }
            });
        } else {
            percorsi.forEach(FascicoloService::deleteFileQuiet);
        }
    }

    private static void deleteFileQuiet(String percorso) {
        try {
            Files.deleteIfExists(Paths.get(percorso));
        } catch (IOException e) {
            // Best-effort, ma tracciato: un PDF sanitario può restare su disco dopo il commit → WARN (minore A6).
            log.warn("A6: unlink PDF fascicolo fallito dopo il commit, il file potrebbe restare su disco: {}", percorso, e);
        }
    }

    /**
     * Invia via email il documento del fascicolo con destinatario vincolato all'email registrata
     * del cliente (override solo con conferma esplicita). {@code @Transactional} (di classe) + cliente
     * LAZY letto entro la sessione.
     */
    public void shareDocumento(Long id, ShareRequest req) {
        DocumentoFascicolo doc = ownershipValidator.getOwnedDocumentoFascicolo(id);
        limitazioneValidator.assertNonLimitato(doc.getCliente()); // A5.3: nessun invio a terzi per cliente limitato
        Long clienteId = doc.getCliente() == null ? null : doc.getCliente().getId();
        String clienteEmail = doc.getCliente() == null ? null : doc.getCliente().getEmail();
        String to = ShareRecipient.resolve(clienteEmail, req);
        // Legge i byte via helper (NON downloadDocumento) per non generare un evento DOWNLOAD spurio:
        // questo flusso è uno SHARE, tracciato una sola volta sotto.
        byte[] pdf = leggiBytesDocumento(doc);
        auditService.recordCriticalNewTx(AuditAction.SHARE, AuditEntityType.DOCUMENTO_FASCICOLO,
                id, clienteId, to, AuditOutcome.SUCCESS);
        try {
            emailService.sendPdfEmail(
                    to,
                    "Il tuo documento: " + doc.getTitolo(),
                    "In allegato trovi il documento richiesto in formato PDF.",
                    pdf,
                    sanitizeFilename(doc.getTitolo()) + ".pdf");
            // Promemoria "già inviato": persistito solo a invio riuscito (entity managed,
            // flush automatico al commit della tx di classe).
            doc.setDataUltimoInvio(java.time.Instant.now());
        } catch (RuntimeException e) {
            auditService.recordCriticalNewTx(AuditAction.SHARE, AuditEntityType.DOCUMENTO_FASCICOLO,
                    id, clienteId, to, AuditOutcome.FAILURE);
            throw e;
        }
    }

    private String sanitizeFilename(String origin) {
        if (origin == null) return "documento";
        return origin.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private DocumentoFascicoloDto toDto(DocumentoFascicolo entity) {
        DocumentoFascicoloDto dto = new DocumentoFascicoloDto();
        dto.setId(entity.getId());
        dto.setClienteId(entity.getCliente().getId());
        dto.setTitolo(entity.getTitolo());
        dto.setTipoDocumento(entity.getTipoDocumento());
        dto.setRiferimentoId(entity.getRiferimentoId());
        dto.setDataCreazione(entity.getDataCreazione());
        dto.setDataUltimoInvio(entity.getDataUltimoInvio());
        return dto;
    }
}
