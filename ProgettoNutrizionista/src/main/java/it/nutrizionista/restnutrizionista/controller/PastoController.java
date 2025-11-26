package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.service.PastoService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/pasti")
public class PastoController {

	@Autowired private PastoService service;
}
