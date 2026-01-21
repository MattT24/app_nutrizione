package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.dto.PastoFormDto;
import it.nutrizionista.restnutrizionista.service.PastoService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/pasti")
public class PastoController {

	@Autowired private PastoService service;
	
	
	@PostMapping
	@PreAuthorize("hasAuthority('PASTO_CREATE')")
	public ResponseEntity<PastoDto> add(@Valid @RequestBody PastoFormDto form){
		var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
	
	@PutMapping
    @PreAuthorize("hasAuthority('PASTO_UPDATE')")
	public ResponseEntity<PastoDto> update(@Valid @RequestBody PastoFormDto form){
		var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

	@DeleteMapping
	@PreAuthorize("hasAuthority('PASTO_DELETE')")
	public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
		service.delete(req.getId());
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	@PreAuthorize("hasAuthority('PASTO_READ')")
	public PageResponse<PastoDto> allMyPasti(Pageable pageable){ 
		return service.listAllMyPasti(pageable);
	}
	
	@GetMapping("/dettaglio")
	@PreAuthorize("hasAuthority('PASTO_DETTAGLIO')")
	public ResponseEntity<PastoDto> dettaglio(@RequestBody IdRequest id){
		var dto = service.dettaglio(id.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
}

