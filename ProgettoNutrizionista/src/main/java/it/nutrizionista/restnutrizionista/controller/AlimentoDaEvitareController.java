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

import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareFormDto;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.NomeRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.AlimentoDaEvitareService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_da_evitare")
public class AlimentoDaEvitareController {

	@Autowired private AlimentoDaEvitareService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_CREATE')")
	public ResponseEntity<AlimentoDaEvitareDto> add(@Valid @RequestBody AlimentoDaEvitareFormDto form){
		var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
	
	@PutMapping
    @PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_UPDATE')")
	public ResponseEntity<AlimentoDaEvitareDto> update(@Valid @RequestBody AlimentoDaEvitareFormDto form){
		var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

	@DeleteMapping
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_DELETE')")
	public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
		service.delete(req.getId());
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_READ')")
	public PageResponse<AlimentoDaEvitareDto> allAlimentiDaEvitare(Pageable pageable){ 
		return service.listAll(pageable);
	} 
	
	@GetMapping("/dettaglio")
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_DETTAGLIO')")
	public ResponseEntity<AlimentoDaEvitareDto> dettaglio(@RequestBody IdRequest id){
		var dto = service.dettaglio(id.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
	
	@GetMapping("/byId")
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_READ')")
	public ResponseEntity<AlimentoDaEvitareDto> getById(@Valid @RequestBody IdRequest req){
		var dto = service.getById(req.getId());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	}
	
	@GetMapping("/byNome")
	@PreAuthorize("hasAuthority('ALIMENTO_DA_EVITARE_READ')")
	public ResponseEntity<AlimentoDaEvitareDto> getByNome(@Valid @RequestBody NomeRequest nome){
		var dto = service.getByNome(nome.getNome());
		return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
	 }
	
}
