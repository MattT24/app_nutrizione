package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // Import all

import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.AlimentoDaEvitareService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_da_evitare")
public class AlimentoDaEvitareController {

	@Autowired private AlimentoDaEvitareService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_CREATE')")
	public ResponseEntity<AlimentoDaEvitareDto> add(@Valid @RequestBody AlimentoDaEvitareFormDto form){
		var create = service.create(form);
		return ResponseEntity.status(201).body(create);
	}
	
	@PutMapping
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_UPDATE')")
	public ResponseEntity<AlimentoDaEvitareDto> update(@Valid @RequestBody AlimentoDaEvitareFormDto form){
		var updated = service.update(form);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_DELETE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
	
    // Tutti gli alimenti da evitare di UN CLIENTE specifico
    // GET /api/alimenti_da_evitare/cliente/123
	@GetMapping("/cliente/{clienteId}")
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_READ')")
	public ResponseEntity<PageResponse<AlimentoDaEvitareDto>> getByCliente(
            @PathVariable Long clienteId, 
            Pageable pageable) {
		return ResponseEntity.ok(service.listByCliente(clienteId, pageable));
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_READ')")
	public ResponseEntity<AlimentoDaEvitareDto> getById(@PathVariable Long id){
		var dto = service.getById(id);
		return ResponseEntity.ok(dto);
	}
}