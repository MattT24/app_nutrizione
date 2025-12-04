package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoFormDto;
import it.nutrizionista.restnutrizionista.dto.GruppoDto;
import it.nutrizionista.restnutrizionista.dto.GruppoPermessiRequest;
import it.nutrizionista.restnutrizionista.service.AlimentoPastoService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_pasto")
public class AlimentoPastoController {

	@Autowired private AlimentoPastoService service;
	

}
