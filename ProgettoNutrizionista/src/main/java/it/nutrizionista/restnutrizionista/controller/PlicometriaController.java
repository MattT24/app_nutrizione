package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PlicometriaDto;
import it.nutrizionista.restnutrizionista.dto.PlicometriaFormDto;
import it.nutrizionista.restnutrizionista.service.PlicometriaService;
import it.nutrizionista.restnutrizionista.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import it.nutrizionista.restnutrizionista.service.EmailService;
import it.nutrizionista.restnutrizionista.dto.ShareRequest;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/plicometrie")
public class PlicometriaController {

    @Autowired 
    private PlicometriaService service;
	@Autowired
	private PdfService pdfService;
	@Autowired
	private EmailService emailService;
    
    @PostMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_CREATE')")
    public ResponseEntity<PlicometriaDto> add(@Valid @RequestBody PlicometriaFormDto form){
        var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
    
    @PutMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_UPDATE')")
    public ResponseEntity<PlicometriaDto> update(@Valid @RequestBody PlicometriaFormDto form){
        var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_DELETE')")
    public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
        service.delete(req.getId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_READ')")
    public PageResponse<PlicometriaDto> allPlicometrieByCliente(
            @RequestParam("clienteId") Long clienteId, Pageable pageable){ 
        return service.allPlicometrieByCliente(clienteId, pageable);
    }

	@GetMapping("/{id}/pdf")
	@PreAuthorize("hasAuthority('PLICOMETRIA_READ')")
	public ResponseEntity<byte[]> getPdf(@PathVariable("id") Long id) {
		byte[] pdf = pdfService.generaPdfPlicometria(id);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"plicometria_" + id + ".pdf\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdf);
	}

	@PostMapping("/{id}/share")
	@PreAuthorize("hasAuthority('PLICOMETRIA_READ')")
	public ResponseEntity<java.util.Map<String, String>> sharePdfViaEmail(@PathVariable("id") Long id, @Valid @RequestBody ShareRequest req) {
		byte[] pdf = pdfService.generaPdfPlicometria(id);
		emailService.sendPdfEmail(
				req.getEmail(),
				"Il tuo report di Plicometria",
				"In allegato trovi il tuo report di plicometria in formato PDF.",
				pdf,
				"plicometria_" + id + ".pdf"
		);
		return ResponseEntity.ok(java.util.Map.of("message", "Email inviata con successo!"));
	}

}