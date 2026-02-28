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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.PresetObiettivoDto;
import it.nutrizionista.restnutrizionista.service.PresetObiettivoService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/preset-obiettivo")
public class PresetObiettivoController {

	@Autowired
	private PresetObiettivoService service;

	@GetMapping
	@PreAuthorize("hasAuthority('CLIENTE_DETTAGLIO')")
	public ResponseEntity<List<PresetObiettivoDto>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	@PostMapping
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<PresetObiettivoDto> crea(@RequestBody PresetObiettivoDto dto) {
		return ResponseEntity.ok(service.crea(dto));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
