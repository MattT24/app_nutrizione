package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplatePatchDto;
import it.nutrizionista.restnutrizionista.service.PastoSchedaTemplateService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/schede-template/{templateId}/alimenti")
public class AlimentoPastoSchedaTemplateController {

	@Autowired
	private PastoSchedaTemplateService service;

	/** PATCH /{aptId} — Aggiorna quantità / nomeCustom di un alimento */
	@PatchMapping("/{aptId}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoPastoSchedaTemplateDto> updateAlimento(
			@PathVariable Long templateId,
			@PathVariable Long aptId,
			@Valid @RequestBody AlimentoPastoSchedaTemplatePatchDto dto) {
		return ResponseEntity.ok(service.updateAlimento(templateId, aptId, dto));
	}

	/** DELETE /{aptId} — Rimuove un alimento dal pasto */
	@DeleteMapping("/{aptId}")
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<Void> deleteAlimento(
			@PathVariable Long templateId,
			@PathVariable Long aptId) {
		service.deleteAlimento(templateId, aptId);
		return ResponseEntity.noContent().build();
	}
}
