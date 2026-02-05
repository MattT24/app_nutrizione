package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // Import all

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoRequest;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.service.AlimentoPastoService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_pasto")
public class AlimentoPastoController {

	@Autowired private AlimentoPastoService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('PASTO_UPDATE')") // Modificare un pasto richiede PASTO_UPDATE
	public ResponseEntity<PastoDto> associaAlimentoAPasto(@Valid @RequestBody AlimentoPastoRequest req ){
		var create = service.associaAlimento(req);
		return ResponseEntity.status(201).body(create);
	}
	
    // GET /api/alimenti_pasto/byPasto/123
	@GetMapping("/byPasto/{pastoId}")
	@PreAuthorize("hasAuthority('PASTO_READ')")
	public List<AlimentoBaseDto> listByPasto(@PathVariable Long pastoId) {
		return service.listAlimentiByPasto(pastoId);
	}
	
    // DELETE /api/alimenti_pasto?pastoId=1&alimentoId=2
	@DeleteMapping
	@PreAuthorize("hasAuthority('PASTO_UPDATE')")
	public ResponseEntity<PastoDto> delete(
	        @RequestParam Long pastoId, 
	        @RequestParam Long alimentoId) {
	    return ResponseEntity.ok(service.eliminaAssociazione(pastoId, alimentoId));
	}
	@PutMapping
	@PreAuthorize("hasAuthority('PASTO_UPDATE')")
	public ResponseEntity<PastoDto> aggiornaQuantita(@Valid @RequestBody AlimentoPastoRequest req){
		var update = service.aggiornaQuantita(req);
		return ResponseEntity.ok(update);
	}
}