package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/clienti")
public class ClienteController {

	@Autowired private ClienteService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('CLIENTE_CREATE')")
	public ResponseEntity<ClienteDto> addCliente(@Valid @RequestBody ClienteFormDto form){
		var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
	
	@GetMapping
	@PreAuthorize("hasAuthority('CLIENTE_DETTAGLIO')")
	public ResponseEntity<ClienteDto> clienteDettaglio(@RequestBody IdRequest id){
		var dto = service.dettaglio(id.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }
	
	
	
}
