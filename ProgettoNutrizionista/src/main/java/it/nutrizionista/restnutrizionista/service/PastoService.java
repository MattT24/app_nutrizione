package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.PastoRepository;

@Service
public class PastoService {

	@Autowired private PastoRepository repo;
}
