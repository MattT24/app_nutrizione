package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.MacroRepository;

@Service
public class MacroService {

	@Autowired private MacroRepository repo;
}
