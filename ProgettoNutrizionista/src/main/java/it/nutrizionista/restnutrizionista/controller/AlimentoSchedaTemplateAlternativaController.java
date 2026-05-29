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

import it.nutrizionista.restnutrizionista.dto.AlimentoSchedaTemplateAlternativaDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoSchedaTemplateAlternativaFormDto;
import it.nutrizionista.restnutrizionista.dto.DisplayNameRequest;
import it.nutrizionista.restnutrizionista.service.AlimentoSchedaTemplateAlternativaService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/schede-template/{templateId}/alimenti/{aptId}/alternative")
public class AlimentoSchedaTemplateAlternativaController {

	@Autowired
	private AlimentoSchedaTemplateAlternativaService service;

	/** GET — Lista alternative per un alimento nel pasto template */
	@GetMapping
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<List<AlimentoSchedaTemplateAlternativaDto>> list(
			@PathVariable Long templateId,
			@PathVariable Long aptId) {
		return ResponseEntity.ok(service.listByAlimentoPasto(templateId, aptId));
	}

	/** POST — Aggiunge una nuova alternativa */
	@PostMapping
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoSchedaTemplateAlternativaDto> create(
			@PathVariable Long templateId,
			@PathVariable Long aptId,
			@Valid @RequestBody AlimentoSchedaTemplateAlternativaFormDto form) {
		return ResponseEntity.status(201).body(service.create(templateId, aptId, form));
	}

	/** PATCH /{altId} — Aggiorna un'alternativa */
	@PatchMapping("/{altId}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoSchedaTemplateAlternativaDto> update(
			@PathVariable Long templateId,
			@PathVariable Long aptId,
			@PathVariable Long altId,
			@Valid @RequestBody AlimentoSchedaTemplateAlternativaFormDto form) {
		return ResponseEntity.ok(service.update(templateId, aptId, altId, form));
	}

	/** DELETE /{altId} — Rimuove un'alternativa */
	@DeleteMapping("/{altId}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<Void> delete(
			@PathVariable Long templateId,
			@PathVariable Long aptId,
			@PathVariable Long altId) {
		service.delete(templateId, aptId, altId);
		return ResponseEntity.noContent().build();
	}

	/** PUT /{altId}/display-name — Imposta nome custom */
	@PutMapping("/{altId}/display-name")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoSchedaTemplateAlternativaDto> setDisplayName(
			@PathVariable Long templateId,
			@PathVariable Long aptId,
			@PathVariable Long altId,
			@Valid @RequestBody DisplayNameRequest body) {
		return ResponseEntity.ok(service.setDisplayName(templateId, aptId, altId, body.getNome()));
	}

	/** DELETE /{altId}/display-name — Reset nome custom */
	@DeleteMapping("/{altId}/display-name")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoSchedaTemplateAlternativaDto> deleteDisplayName(
			@PathVariable Long templateId,
			@PathVariable Long aptId,
			@PathVariable Long altId) {
		return ResponseEntity.ok(service.deleteDisplayName(templateId, aptId, altId));
	}
}
