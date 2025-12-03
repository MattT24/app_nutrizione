package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.service.PlicometriaService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/plicometrie")
public class PlicometriaController {

	@Autowired private PlicometriaService service;
	
	
	
}
