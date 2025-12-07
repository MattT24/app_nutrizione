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

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.CognomeRequest;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.NomeRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.ClienteService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/clienti")
public class ClienteController {

	@Autowired private ClienteService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('CLIENTE_CREATE')")
	public ResponseEntity<ClienteDto> add(@Valid @RequestBody ClienteFormDto form){
		var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
	
	@PutMapping
    @PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<ClienteDto> update(@Valid @RequestBody ClienteFormDto form){
		var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

	@DeleteMapping
	@PreAuthorize("hasAuthority('CLIENTE_DELETE')")
	public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
		service.delete(req.getId());
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public PageResponse<ClienteDto> allMyClienti(Pageable pageable){ 
		return service.listAll(pageable);
	} 
	
	@GetMapping("/byId")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public ResponseEntity<ClienteDto> getById(@Valid @RequestBody IdRequest req){
		var dto = service.getById(req.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
	
	@GetMapping("/byNome")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public ResponseEntity<ClienteDto> getByNome(@Valid @RequestBody NomeRequest nome){
		var dto = service.getByNome(nome.getNome());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	 }
	
	
	@GetMapping("/byCognome")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public ResponseEntity<ClienteDto> getByCognome(@Valid @RequestBody CognomeRequest cognome){
		var dto = service.getByCognome(cognome.getCognome());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	 }
	
	
	@GetMapping("/dettaglio")
	@PreAuthorize("hasAuthority('CLIENTE_DETTAGLIO')")
	public ResponseEntity<ClienteDto> dettaglio(@RequestBody IdRequest id){
		var dto = service.dettaglio(id.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
	
	//manca cliente Fabbisogno, da studiare un attimo
	
}
