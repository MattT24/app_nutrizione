package it.nutrizionista.restnutrizionista.controller;

import it.nutrizionista.restnutrizionista.dto.DocumentoFascicoloDto;
import it.nutrizionista.restnutrizionista.dto.SalvaDocumentoRequest;
import it.nutrizionista.restnutrizionista.dto.ShareRequest;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.service.FascicoloService;
import it.nutrizionista.restnutrizionista.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/fascicolo")
public class FascicoloController {

    @Autowired
    private FascicoloService service;
    
    @Autowired
    private EmailService emailService;

    @PostMapping("/salva")
    @PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
    public ResponseEntity<DocumentoFascicoloDto> salvaDocumento(@Valid @RequestBody SalvaDocumentoRequest request) {
        return ResponseEntity.status(201).body(service.salvaDocumento(request));
    }

    @GetMapping("/cliente")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public ResponseEntity<List<DocumentoFascicoloDto>> getDocumentiByCliente(@RequestParam("clienteId") Long clienteId) {
        return ResponseEntity.ok(service.getDocumentiByCliente(clienteId));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id) {
        DocumentoFascicolo entity = service.getDocumentoEntity(id);
        byte[] pdf = service.downloadDocumento(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeFilename(entity.getTitolo()) + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/share")
    @PreAuthorize("hasAuthority('CLIENTE_READ')")
    public ResponseEntity<Map<String, String>> shareViaEmail(@PathVariable("id") Long id, @Valid @RequestBody ShareRequest req) {
        DocumentoFascicolo entity = service.getDocumentoEntity(id);
        byte[] pdf = service.downloadDocumento(id);
        emailService.sendPdfEmail(
                req.getEmail(),
                "Il tuo documento: " + entity.getTitolo(),
                "In allegato trovi il documento richiesto in formato PDF.",
                pdf,
                sanitizeFilename(entity.getTitolo()) + ".pdf"
        );
        return ResponseEntity.ok(Map.of("message", "Email inviata con successo!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.eliminaDocumento(id);
        return ResponseEntity.noContent().build();
    }
    
    private String sanitizeFilename(String origin) {
        if(origin == null) return "documento";
        return origin.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
