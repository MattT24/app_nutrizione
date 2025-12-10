package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.MicroDto;
import it.nutrizionista.restnutrizionista.dto.NomeRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.MicroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/micronutrienti")
public class MicroController {

	@Autowired private MicroService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('MICRO_READ')")
	public PageResponse<MicroDto> allMicro(Pageable pageable){ 
		return service.listAll(pageable);
	} 
	@GetMapping("/nome")
	@PreAuthorize("hasAuthority('MICRO_READ')")
	public ResponseEntity<MicroDto> getByNome(@Valid @RequestBody NomeRequest nome){
		var dto = service.getByNome(nome.getNome());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	 }
}
