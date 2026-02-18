package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoDto;
import it.nutrizionista.restnutrizionista.dto.DisplayNameRequest;
import it.nutrizionista.restnutrizionista.service.AlimentoPastoDisplayNameService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schede/{schedaId}/alimenti-pasto/{alimentoPastoId}/display-name")
public class AlimentoPastoDisplayNameController {
	@Autowired private AlimentoPastoDisplayNameService service;

	@PutMapping
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoPastoDto> set(@PathVariable Long schedaId, @PathVariable Long alimentoPastoId, @Valid @RequestBody DisplayNameRequest req) {
		return ResponseEntity.ok(service.setDisplayName(schedaId, alimentoPastoId, req.getNome()));
	}

	@DeleteMapping
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<AlimentoPastoDto> delete(@PathVariable Long schedaId, @PathVariable Long alimentoPastoId) {
		return ResponseEntity.ok(service.deleteDisplayName(schedaId, alimentoPastoId));
	}
}
