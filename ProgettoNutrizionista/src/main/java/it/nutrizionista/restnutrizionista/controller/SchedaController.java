package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.CopyDayRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.service.SchedaService;
import it.nutrizionista.restnutrizionista.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import it.nutrizionista.restnutrizionista.service.EmailService;
import it.nutrizionista.restnutrizionista.dto.ShareRequest;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/schede")
public class SchedaController {

	@Autowired private SchedaService service;
	@Autowired private PdfService pdfService;
	@Autowired private EmailService emailService;

	@PostMapping
	@PreAuthorize("hasAuthority('SCHEDA_CREATE')")
	public ResponseEntity<SchedaDto> addScheda(@Valid @RequestBody SchedaFormDto form){
		var create = service.create(form);
		return ResponseEntity.status(201).body(create);
	}

	@PutMapping
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<SchedaDto> updateScheda(@Valid @RequestBody SchedaFormDto form){
		var updated = service.update(form);
		return ResponseEntity.status(201).body(updated);
	}

	@DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDA_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
	
	@GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDA_READ')")
    public ResponseEntity<SchedaDto> getById(@PathVariable("id") Long id){
        var dto = service.getById(id); // Assicurati che il service abbia getById(Long)
        return ResponseEntity.ok(dto);
    }

	@GetMapping("/cliente")
    @PreAuthorize("hasAuthority('SCHEDA_READ')")
    public ResponseEntity<PageResponse<SchedaDto>> schedeByCliente(@RequestParam("clienteId") Long clienteId, Pageable pageable){
        return ResponseEntity.ok(service.schedeByCliente(clienteId, pageable));
    }

	// 1. Duplica la scheda PER LO STESSO CLIENTE (Clone rapido)
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('SCHEDA_CREATE')")
    public ResponseEntity<SchedaDto> duplicateScheda(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.duplicateScheda(id));
    }

    // 2. Importa la scheda SU UN ALTRO CLIENTE (Clone con controlli)
    // POST /api/schede/import?sourceId=123&targetClienteId=45
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SCHEDA_CREATE')")
    public ResponseEntity<SchedaDto> importFromCliente(
            @RequestParam("sourceId") Long sourceId, 
            @RequestParam("targetClienteId") Long targetClienteId) {
        
        return ResponseEntity.ok(service.duplicateFromCliente(sourceId, targetClienteId));
    }
    
    // 3. Copia giorno
    @PostMapping("/{id}/copy-day")
    @PreAuthorize("hasAuthority('SCHEDA_CREATE')")
    public ResponseEntity<SchedaDto> copyDay(@PathVariable("id") Long id, @Valid @RequestBody CopyDayRequest request) {
        return ResponseEntity.ok(service.copyDay(id, request));
    }

    //ha senso dare la possibilità di attivare una scheda diversa dall'ultima creata? Lo userei più come flag nel frontend che come funzionalità
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
    public ResponseEntity<SchedaDto> activateScheda(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.activateScheda(id));
    }

	@GetMapping("/{id}/pdf")
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<byte[]> getPdf(@PathVariable("id") Long id) {
		byte[] pdf = pdfService.generaPdfScheda(id);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"scheda_" + id + ".pdf\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdf);
	}

	@PostMapping("/{id}/share")
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<java.util.Map<String, String>> sharePdfViaEmail(@PathVariable("id") Long id, @Valid @RequestBody ShareRequest req) {
		byte[] pdf = pdfService.generaPdfScheda(id);
		emailService.sendPdfEmail(
				req.getEmail(),
				"La tua Scheda Nutrizionale",
				"In allegato trovi la tua scheda nutrizionale in formato PDF.",
				pdf,
				"scheda_" + id + ".pdf"
		);
		return ResponseEntity.ok(java.util.Map.of("message", "Email inviata con successo!"));
	}
}
