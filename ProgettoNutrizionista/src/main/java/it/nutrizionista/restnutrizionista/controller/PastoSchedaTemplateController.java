package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplateCreateDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoSchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoSchedaTemplateFormDto;
import it.nutrizionista.restnutrizionista.dto.ReorderDto;
import it.nutrizionista.restnutrizionista.service.PastoSchedaTemplateService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/schede-template/{templateId}/pasti")
public class PastoSchedaTemplateController {

	@Autowired
	private PastoSchedaTemplateService service;

	/** POST — Crea un nuovo pasto vuoto nel template */
	@PostMapping
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<PastoSchedaTemplateDto> createPasto(
			@PathVariable Long templateId,
			@Valid @RequestBody PastoSchedaTemplateFormDto dto) {
		return ResponseEntity.status(201).body(service.createPasto(templateId, dto));
	}

	/** PATCH /{pastoId} — Aggiorna nome, descrizione, orari del pasto */
	@PatchMapping("/{pastoId}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<PastoSchedaTemplateDto> updatePasto(
			@PathVariable Long templateId,
			@PathVariable Long pastoId,
			@Valid @RequestBody PastoSchedaTemplateFormDto dto) {
		return ResponseEntity.ok(service.updatePasto(templateId, pastoId, dto));
	}

	/** DELETE /{pastoId} — Rimuove il pasto */
	@DeleteMapping("/{pastoId}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<Void> deletePasto(
			@PathVariable Long templateId,
			@PathVariable Long pastoId) {
		service.deletePasto(templateId, pastoId);
		return ResponseEntity.noContent().build();
	}

	/** PATCH /reorder — Persiste ordine drag-and-drop pasti */
	@PatchMapping("/reorder")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<Void> reorderPasti(
			@PathVariable Long templateId,
			@Valid @RequestBody ReorderDto dto) {
		service.reorderPasti(templateId, dto);
		return ResponseEntity.noContent().build();
	}

	/** POST /{pastoId}/alimenti — Aggiunge un alimento dal catalogo al pasto */
	@PostMapping("/{pastoId}/alimenti")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoPastoSchedaTemplateDto> addAlimento(
			@PathVariable Long templateId,
			@PathVariable Long pastoId,
			@Valid @RequestBody AlimentoPastoSchedaTemplateCreateDto dto) {
		return ResponseEntity.status(201).body(service.addAlimento(templateId, pastoId, dto));
	}

	/** PATCH /{pastoId}/alimenti/reorder — Persiste ordine drag-and-drop alimenti */
	@PatchMapping("/{pastoId}/alimenti/reorder")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<Void> reorderAlimenti(
			@PathVariable Long templateId,
			@PathVariable Long pastoId,
			@Valid @RequestBody ReorderDto dto) {
		service.reorderAlimenti(templateId, pastoId, dto);
		return ResponseEntity.noContent().build();
	}
}
