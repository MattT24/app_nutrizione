package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.IdRequest;
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

	@DeleteMapping
	@PreAuthorize("hasAuthority('SCHEDA_DELETE')")
	public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
		service.delete(req.getId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/byId")
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<SchedaDto> getById(@Valid @RequestBody IdRequest req){
		var dto = service.getById(req.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}

	//non so a che serve questa
	@GetMapping("/pasti")
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public ResponseEntity<SchedaDto> pastiByScheda(@Valid @RequestBody IdRequest req){
		var dto = service.pastiByScheda(req.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}

	@GetMapping("/byCliente")
	@PreAuthorize("hasAuthority('SCHEDA_READ')")
	public List<SchedaDto> schedeByCliente(@Valid @RequestBody IdRequest req){
		var dto = service.schedeByCliente(req.getId());
		return dto;
	}

	// 1. Duplica la scheda PER LO STESSO CLIENTE (Clone rapido)
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('SCHEDA_CREATE')")
    public SchedaDto duplicateScheda(@Valid @RequestBody IdRequest req) {
        return service.duplicateScheda(req.getId());
    }

    // 2. Importa la scheda SU UN ALTRO CLIENTE (Clone con controlli)
    // POST /api/schede/import?sourceId=123&targetClienteId=45
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SCHEDA_CREATE')")
    public SchedaDto importFromCliente(@Valid @RequestBody IdRequest req1, @RequestBody IdRequest req2) {
        return service.duplicateFromCliente(req1.getId(), req2.getId());
    }
}
