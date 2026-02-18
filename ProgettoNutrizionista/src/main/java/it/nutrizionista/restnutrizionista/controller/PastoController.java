package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // Importa tutto

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.dto.PastoFormDto;
import it.nutrizionista.restnutrizionista.dto.PastoOrariFormDto;
import it.nutrizionista.restnutrizionista.service.PastoService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/pasti")
public class PastoController {

	@Autowired private PastoService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('PASTO_CREATE')")
	public ResponseEntity<PastoDto> create(@Valid @RequestBody PastoFormDto form){
		var create = service.create(form);
		return ResponseEntity.status(201).body(create);
	}
	
	@PutMapping
	@PreAuthorize("hasAuthority('PASTO_UPDATE')")
	public ResponseEntity<PastoDto> update(@Valid @RequestBody PastoFormDto form){
		var updated = service.update(form);
		return ResponseEntity.ok(updated);
	}
	
	@PutMapping("/{id}/orari")
	@PreAuthorize("hasAuthority('PASTO_UPDATE')")
	public ResponseEntity<PastoDto> updateOrari(@PathVariable Long id, @Valid @RequestBody PastoOrariFormDto form){
		var updated = service.updateOrari(id, form.getOrarioInizio(), form.getOrarioFine());
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('PASTO_DELETE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
	
    // Tutti i pasti del nutrizionista (poco usato ma ok)
	@GetMapping
	@PreAuthorize("hasAuthority('PASTO_READ')")
	public ResponseEntity<PageResponse<PastoDto>> allMyPasti(Pageable pageable){
		return ResponseEntity.ok(service.listAllMyPasti(pageable));
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('PASTO_READ')")
	public ResponseEntity<PastoDto> getById(@PathVariable Long id){
		var dto = service.dettaglio(id); // Uso dettaglio per avere tutto
		return ResponseEntity.ok(dto);
	}
}
