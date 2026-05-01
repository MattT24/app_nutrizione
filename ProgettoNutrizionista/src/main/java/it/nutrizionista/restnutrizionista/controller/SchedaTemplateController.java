package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.ApplicaSchedaTemplateRequest;
import it.nutrizionista.restnutrizionista.dto.CopyDayRequest;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateListDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateMetadataPatchDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateUpsertDto;
import it.nutrizionista.restnutrizionista.service.SchedaTemplateService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/schede-template")
public class SchedaTemplateController {

	@Autowired
	private SchedaTemplateService service;

	/** GET /api/schede-template — Lista template leggera (solo metadati per card) */
	@GetMapping
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<List<SchedaTemplateListDto>> listMine() {
		return ResponseEntity.ok(service.listMine());
	}

	/** GET /api/schede-template/summary — Lista leggera (solo id, nome, tipo) per dropdown */
	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<List<SchedaTemplateDto>> listMineSummary() {
		return ResponseEntity.ok(service.listMineSummary());
	}

	/** GET /api/schede-template/{id} — Dettaglio template */
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<SchedaTemplateDto> getById(@PathVariable Long id) {
		return ResponseEntity.ok(service.getById(id));
	}

	/** POST /api/schede-template — Crea template */
	@PostMapping
	@PreAuthorize("hasAuthority('SCHEDA_CREATE')")
	public ResponseEntity<SchedaTemplateDto> create(@Valid @RequestBody SchedaTemplateUpsertDto req) {
		return ResponseEntity.status(201).body(service.create(req));
	}

	/** PUT /api/schede-template/{id} — Aggiorna template (legacy, solo metadata) */
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<SchedaTemplateDto> update(@PathVariable Long id,
			@Valid @RequestBody SchedaTemplateUpsertDto req) {
		return ResponseEntity.ok(service.update(id, req));
	}

	/** PATCH /api/schede-template/{id} — Aggiorna solo metadata (nome, descrizione, tipo) */
	@PatchMapping("/{id}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<SchedaTemplateDto> patchMetadata(@PathVariable Long id,
			@Valid @RequestBody SchedaTemplateMetadataPatchDto dto) {
		return ResponseEntity.ok(service.patchMetadata(id, dto));
	}

	/** DELETE /api/schede-template/{id} — Elimina template */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('SCHEDA_DELETE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	/** POST /api/schede-template/{templateId}/applica/{schedaId} — Applica template su scheda esistente */
	@PostMapping("/{templateId}/applica/{schedaId}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<SchedaDto> applicaAScheda(
			@PathVariable Long templateId,
			@PathVariable Long schedaId,
			@Valid @RequestBody ApplicaSchedaTemplateRequest req) {
		return ResponseEntity.ok(service.applicaAScheda(templateId, schedaId, req));
	}

	/** POST /api/schede-template/{templateId}/crea-scheda — Crea scheda cliente da template */
	@PostMapping("/{templateId}/crea-scheda")
	@PreAuthorize("hasAuthority('SCHEDA_CREATE')")
	public ResponseEntity<SchedaDto> creaSchedaDaTemplate(
			@PathVariable Long templateId,
			@Valid @RequestBody SchedaFormDto schedaForm) {
		return ResponseEntity.status(201).body(service.creaSchedaDaTemplate(templateId, schedaForm));
	}

	/** POST /api/schede-template/{templateId}/copy-day — Copia pasti da un giorno ad altri */
	@PostMapping("/{templateId}/copy-day")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<SchedaTemplateDto> copyDay(
			@PathVariable Long templateId,
			@Valid @RequestBody CopyDayRequest request) {
		return ResponseEntity.ok(service.copyDay(templateId, request));
	}
}
