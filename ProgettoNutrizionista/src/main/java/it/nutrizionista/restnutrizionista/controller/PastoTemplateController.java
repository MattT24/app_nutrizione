package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.PastoTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateUpsertDto;
import it.nutrizionista.restnutrizionista.service.PastoTemplateService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/pasti_templates")
public class PastoTemplateController {
	@Autowired private PastoTemplateService service;

	@GetMapping
	@PreAuthorize("hasAuthority('PASTI_TEMPLATE_MANAGE')")
	public ResponseEntity<List<PastoTemplateDto>> listMine() {
		return ResponseEntity.ok(service.listMine());
	}

	@PostMapping
	@PreAuthorize("hasAuthority('PASTI_TEMPLATE_MANAGE')")
	public ResponseEntity<PastoTemplateDto> create(@Valid @RequestBody PastoTemplateUpsertDto req) {
		return ResponseEntity.status(201).body(service.create(req));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('PASTI_TEMPLATE_MANAGE')")
	public ResponseEntity<PastoTemplateDto> update(@PathVariable Long id, @Valid @RequestBody PastoTemplateUpsertDto req) {
		return ResponseEntity.ok(service.update(id, req));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('PASTI_TEMPLATE_MANAGE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
