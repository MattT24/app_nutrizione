package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;

@Service
public class AlimentoPastoService {

	@Autowired private AlimentoPastoRepository repo;
}
