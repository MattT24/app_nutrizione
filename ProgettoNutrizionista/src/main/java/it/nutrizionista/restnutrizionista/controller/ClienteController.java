package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

	@DeleteMapping("/mio")
	@PreAuthorize("hasAuthority('CLIENTE_MY_DELETE')")
	public ResponseEntity<Void> deleteMyCliente(@RequestBody IdRequest req){
		 service.deleteMyCliente(req.getId());
		 return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public PageResponse<ClienteDto> allMyClienti(@PageableDefault(size = 12, page = 0) Pageable pageable){ 
		return service.allMyClienti( pageable);
	} 
	
	@GetMapping("/byId")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public ResponseEntity<ClienteDto> getById(@Valid @RequestBody IdRequest req){
		var dto = service.getById(req.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
	
	@GetMapping("/byNome")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public List<ClienteDto> getByNome(@Valid @RequestBody NomeRequest nome){
		return service.findByNome(nome.getNome());
	
	 }
		
	@GetMapping("/byCognome")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public List<ClienteDto> getByCognome(@Valid @RequestBody CognomeRequest cognome){
		return service.findByCognome(cognome.getCognome());
	}
	
	@PostMapping("/dettaglio")
	@PreAuthorize("hasAuthority('CLIENTE_DETTAGLIO')")
	public ResponseEntity<ClienteDto> dettaglio(@RequestBody IdRequest id){
		var dto = service.dettaglio(id.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
	
	//manca cliente Fabbisogno, da studiare un attimo
	
}
