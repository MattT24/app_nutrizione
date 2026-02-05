package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.service.SchedaService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/schede")
public class SchedaController {

	@Autowired private SchedaService service;

	@PostMapping
	@PreAuthorize("hasAuthority('SCHEDA_CREATE')")
	public ResponseEntity<SchedaDto> addScheda(@Valid @RequestBody SchedaFormDto form){
		var create = service.create(form);
		return ResponseEntity.status(201).body(create);
	}

	@PutMapping
	@PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
	public ResponseEntity<SchedaDto> updateScheda(@Valid @RequestBody SchedaFormDto form){
		var updated = service.update(form);
		return ResponseEntity.status(201).body(updated);
	}

	@DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDA_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
	
	@GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDA_READ')")
    public ResponseEntity<SchedaDto> getById(@PathVariable Long id){
        var dto = service.getById(id); // Assicurati che il service abbia getById(Long)
        return ResponseEntity.ok(dto);
    }

	@GetMapping("/cliente")
    @PreAuthorize("hasAuthority('SCHEDA_READ')")
    public ResponseEntity<PageResponse<SchedaDto>> schedeByCliente(@RequestParam Long clienteId, Pageable pageable){
        return ResponseEntity.ok(service.schedeByCliente(clienteId, pageable));
    }

	// 1. Duplica la scheda PER LO STESSO CLIENTE (Clone rapido)
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('SCHEDA_CREATE')")
    public ResponseEntity<SchedaDto> duplicateScheda(@PathVariable Long id) {
        return ResponseEntity.ok(service.duplicateScheda(id));
    }

    // 2. Importa la scheda SU UN ALTRO CLIENTE (Clone con controlli)
    // POST /api/schede/import?sourceId=123&targetClienteId=45
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SCHEDA_CREATE')")
    public ResponseEntity<SchedaDto> importFromCliente(
            @RequestParam Long sourceId, 
            @RequestParam Long targetClienteId) {
        
        return ResponseEntity.ok(service.duplicateFromCliente(sourceId, targetClienteId));
    }
    
    //ha senso dare la possibilità di attivare una scheda diversa dall'ultima creata? Lo userei più come flag nel frontend che come funzionalità
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SCHEDA_UPDATE')")
    public ResponseEntity<SchedaDto> activateScheda(@PathVariable Long id) {
        return ResponseEntity.ok(service.activateScheda(id));
    }
}
