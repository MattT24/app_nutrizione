package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.MacroService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/macronutrienti")
public class MacroController {

	@Autowired private MacroService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('MACRO_READ')")
	public PageResponse<MacroDto> allMacro(Pageable pageable){ 
		return service.listAll(pageable);
	}
}
