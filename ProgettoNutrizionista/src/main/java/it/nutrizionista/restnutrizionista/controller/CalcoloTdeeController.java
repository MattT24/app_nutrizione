package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // Import all

import it.nutrizionista.restnutrizionista.dto.CalcoloTdeeDto;
import it.nutrizionista.restnutrizionista.dto.CalcoloTdeeFormDto;
import it.nutrizionista.restnutrizionista.service.CalcoloTdeeService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/tdee")
public class CalcoloTdeeController {

	@Autowired private CalcoloTdeeService service;

	@PostMapping
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')") 
	public ResponseEntity<CalcoloTdeeDto> calcolaTdee(@Valid @RequestBody CalcoloTdeeFormDto form){
		var create = service.calcolaESalva(form);
		return ResponseEntity.status(201).body(create);
	}
	
	// GET /api/tdee/cliente/123
	@GetMapping("/cliente/{clienteId}")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
	public List<CalcoloTdeeDto> getStoricoCliente(@PathVariable Long clienteId) {
		return service.getStoricoCliente(clienteId);
	}
	
	@GetMapping("/recenti")
	@PreAuthorize("hasAuthority('CLIENTE_READ')")
    public ResponseEntity<List<CalcoloTdeeDto>> getUltimiCalcoli() {
        return ResponseEntity.ok(service.getUltimiCalcoli());
    }
	
	// DELETE /api/tdee/123
	@DeleteMapping("/{calcoloId}")
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<Void> delete(@PathVariable Long calcoloId) {
	    service.eliminaCalcolo(calcoloId);
	    return ResponseEntity.ok().build();
	}
	
	// DELETE /api/tdee/cliente/123
		@DeleteMapping("/cliente/{clienteId}")
		@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
		public ResponseEntity<Void> deleteAllByCliente(@PathVariable Long clienteId) {
		    service.eliminaTuttiCalcoliCliente(clienteId);
		    return ResponseEntity.ok().build();
		}
}