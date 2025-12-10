package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoRequest;
import it.nutrizionista.restnutrizionista.dto.IdRequest;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.service.AlimentoPastoService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_pasto")
public class AlimentoPastoController {

	@Autowired private AlimentoPastoService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('ALIMENTO_PASTO_CREATE')")
	public ResponseEntity<PastoDto> associaAlimentoAPasto(@RequestBody AlimentoPastoRequest req ){
		var create = service.associaAlimento(req);
		return ResponseEntity.status(201).body(create);
	}
    @GetMapping("/alimenti")
    @PreAuthorize("hasAuthority('ALIMENTO_PASTO_READ')")
    public List<AlimentoBaseDto> listByPasto(@RequestBody IdRequest req) {
        return service.listAlimentyByPasto(req.getId());
    }
    
    @DeleteMapping
    @PreAuthorize("hasAuthority('ALIMENTO_PASTO_DELETE')")
    public ResponseEntity<PastoDto> delete(@Valid @RequestBody AlimentoPastoRequest req) {
    	service.eliminaAssociazione(req);
    	return ResponseEntity.noContent().build();
    }
    
    @PutMapping
    @PreAuthorize("hasAuthority('ALIMENTO_PASTO_UPDATE')")
    public ResponseEntity<PastoDto> aggiornaQuantita(@Valid @RequestBody AlimentoPastoRequest req){
    	var update = service.aggiornaQuantita(req);
    	return ResponseEntity.status(201).body(update);
    }


}
