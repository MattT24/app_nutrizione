package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	
	
}
