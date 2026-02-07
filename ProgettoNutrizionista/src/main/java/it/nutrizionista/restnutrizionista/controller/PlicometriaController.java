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
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PlicometriaDto;
import it.nutrizionista.restnutrizionista.dto.PlicometriaFormDto;
import it.nutrizionista.restnutrizionista.service.PlicometriaService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/plicometrie")
public class PlicometriaController {

    @Autowired 
    private PlicometriaService service;
    
    @PostMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_CREATE')")
    public ResponseEntity<PlicometriaDto> add(@Valid @RequestBody PlicometriaFormDto form){
        var create = service.create(form);
        return ResponseEntity.status(201).body(create);
    }
    
    @PutMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_UPDATE')")
    public ResponseEntity<PlicometriaDto> update(@Valid @RequestBody PlicometriaFormDto form){
        var updated = service.update(form);
        return ResponseEntity.status(201).body(updated);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_DELETE')")
    public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
        service.delete(req.getId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('PLICOMETRIA_READ')")
    public PageResponse<PlicometriaDto> allPlicometrieByCliente(
            @RequestParam Long clienteId, Pageable pageable){ 
        return service.allPlicometrieByCliente(clienteId, pageable);
    }

}