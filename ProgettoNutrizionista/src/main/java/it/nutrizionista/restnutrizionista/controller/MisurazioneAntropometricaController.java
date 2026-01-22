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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.MisurazioneAntropometricaService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/misurazioni_antropometriche")
public class MisurazioneAntropometricaController {

	@Autowired private MisurazioneAntropometricaService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_CREATE')")
	public ResponseEntity<MisurazioneAntropometricaDto> add(@Valid @RequestBody MisurazioneAntropometricaFormDto form){
		var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
	
	@PutMapping
    @PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_UPDATE')")
	public ResponseEntity<MisurazioneAntropometricaDto> update(@Valid @RequestBody MisurazioneAntropometricaFormDto form){
		var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

	@DeleteMapping
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_DELETE')")
	public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
		service.delete(req.getId());
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	@PreAuthorize("hasAuthority('MISURAZIONE_ANTROPOMETRICA_READ')")
	public PageResponse<MisurazioneAntropometricaDto> allMisurazioniByCliente(
	        @RequestParam("clienteId") Long clienteId, Pageable pageable){ 
	    return service.allMisurazioniByCliente(clienteId, pageable);
	}

}
