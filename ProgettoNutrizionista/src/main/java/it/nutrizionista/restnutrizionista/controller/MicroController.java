package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.service.MicroService;

@RestController
@RequestMapping("/api/micronutrienti")
public class MicroController {

	@Autowired private MicroService service;
}
