package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.DocumentoFascicoloDto;
import it.nutrizionista.restnutrizionista.dto.SalvaDocumentoRequest;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ClienteRepository clienteRepository;
    private final PdfService pdfService;
    private final SchedaRepository schedaRepository;
    private final MisurazioneAntropometricaRepository misurazioneRepository;
    private final PlicometriaRepository plicometriaRepository;
    
    private final String uploadDir = "uploads/fascicoli";

    public FascicoloService(DocumentoFascicoloRepository fascicoloRepository, PdfService pdfService,
                            SchedaRepository schedaRepository, MisurazioneAntropometricaRepository misurazioneRepository, PlicometriaRepository plicometriaRepository, ClienteRepository clienteRepository) {
        this.fascicoloRepository = fascicoloRepository;
        this.clienteRepository = clienteRepository;
        this.pdfService = pdfService;
        this.schedaRepository = schedaRepository;
        this.misurazioneRepository = misurazioneRepository;
        this.plicometriaRepository = plicometriaRepository;
    }

    public DocumentoFascicoloDto salvaDocumento(SalvaDocumentoRequest request) {
        // Controllo se esiste già
        var esistente = fascicoloRepository.findByClienteIdAndTipoDocumentoAndRiferimentoId(
                request.getClienteId(), request.getTipoDocumento(), request.getRiferimentoId());
        
        if (esistente.isPresent()) {
            return toDto(esistente.get());
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente non trovato"));

        byte[] pdfBytes;
        String titoloBase = "";

        switch (request.getTipoDocumento()) {
            case SCHEDA:
                Scheda scheda = schedaRepository.findById(request.getRiferimentoId())
                        .orElseThrow(() -> new IllegalArgumentException("Scheda non trovata"));
                pdfBytes = pdfService.generaPdfScheda(scheda.getId());
                titoloBase = "Scheda " + scheda.getNome();
                break;
            case MISURAZIONE:
                MisurazioneAntropometrica misurazione = misurazioneRepository.findById(request.getRiferimentoId())
                        .orElseThrow(() -> new IllegalArgumentException("Misurazione non trovata"));
                pdfBytes = pdfService.generaPdfMisurazione(misurazione.getId());
                titoloBase = "Misurazione del " + misurazione.getDataMisurazione().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                break;
            case PLICOMETRIA:
                Plicometria plicometria = plicometriaRepository.findById(request.getRiferimentoId())
                        .orElseThrow(() -> new IllegalArgumentException("Plicometria non trovata"));
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
        return fascicoloRepository.findByClienteIdOrderByDataCreazioneDesc(clienteId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocumento(Long id) {
        DocumentoFascicolo doc = fascicoloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento non trovato"));
        try {
            Path path = Paths.get(doc.getPercorsoFile());
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Errore durante la lettura del file dal disco", e);
        }
    }
    
    @Transactional(readOnly = true)
    public DocumentoFascicolo getDocumentoEntity(Long id) {
        return fascicoloRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Documento non trovato"));
    }

    public void eliminaDocumento(Long id) {
        DocumentoFascicolo doc = fascicoloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento non trovato"));
        try {
            Path path = Paths.get(doc.getPercorsoFile());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Ignoriamo o loggiamo l'errore del file fisico, ma cancelliamo il record
        }
        fascicoloRepository.delete(doc);
    }

    private DocumentoFascicoloDto toDto(DocumentoFascicolo entity) {
        DocumentoFascicoloDto dto = new DocumentoFascicoloDto();
        dto.setId(entity.getId());
        dto.setClienteId(entity.getCliente().getId());
        dto.setTitolo(entity.getTitolo());
        dto.setTipoDocumento(entity.getTipoDocumento());
        dto.setRiferimentoId(entity.getRiferimentoId());
        dto.setDataCreazione(entity.getDataCreazione());
        return dto;
    }
}
