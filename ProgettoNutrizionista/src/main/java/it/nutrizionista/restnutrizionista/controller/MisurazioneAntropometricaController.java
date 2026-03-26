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
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.MisurazioneAntropometricaService;
import it.nutrizionista.restnutrizionista.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import it.nutrizionista.restnutrizionista.service.EmailService;
import it.nutrizionista.restnutrizionista.dto.ShareRequest;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/misurazioni_antropometriche")
public class MisurazioneAntropometricaController {

	@Autowired private MisurazioneAntropometricaService service;
	@Autowired private PdfService pdfService;
	@Autowired private EmailService emailService;
	
	@PostMapping
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_CREATE')")
	public ResponseEntity<MisurazioneAntropometricaDto> add(@Valid @RequestBody MisurazioneAntropometricaFormDto form){
		var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
	
	@PutMapping
    @PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_UPDATE')")
	public ResponseEntity<MisurazioneAntropometricaDto> update(@Valid @RequestBody MisurazioneAntropometricaFormDto form){
		var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

	@DeleteMapping
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_DELETE')")
	public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
		service.delete(req.getId());
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_READ')")
	public PageResponse<MisurazioneAntropometricaDto> allMisurazioniByCliente(
	        @RequestParam("clienteId") Long clienteId, Pageable pageable){ 
	    return service.allMisurazioniCliente(clienteId, pageable);
	}

	@GetMapping("/{id}/pdf")
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_READ')")
	public ResponseEntity<byte[]> getPdf(@PathVariable("id") Long id) {
		byte[] pdf = pdfService.generaPdfMisurazione(id);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"misurazione_" + id + ".pdf\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdf);
	}

	@PostMapping("/{id}/share")
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_READ')")
	public ResponseEntity<java.util.Map<String, String>> sharePdfViaEmail(@PathVariable("id") Long id, @Valid @RequestBody ShareRequest req) {
		byte[] pdf = pdfService.generaPdfMisurazione(id);
		emailService.sendPdfEmail(
				req.getEmail(),
				"Il tuo report di Misurazione Antropometrica",
				"In allegato trovi il tuo report di misurazione antropometrica in formato PDF.",
				pdf,
				"misurazione_" + id + ".pdf"
		);
		return ResponseEntity.ok(java.util.Map.of("message", "Email inviata con successo!"));
	}

}
