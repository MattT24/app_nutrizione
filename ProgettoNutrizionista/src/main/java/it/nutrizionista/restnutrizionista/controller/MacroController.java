package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.service.MacroService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/macronutrienti")
public class MacroController {

	@Autowired private MacroService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('MACRO_READ')")
	public ResponseEntity<MacroDto> getByAlimento(@Valid @RequestBody IdRequest req){ 
		var dto = service.getByAlimento(req.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
}
