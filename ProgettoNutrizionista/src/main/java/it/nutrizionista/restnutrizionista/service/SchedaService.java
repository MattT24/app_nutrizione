package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.SchedaRepository;

@Service
public class SchedaService {

	@Autowired private SchedaRepository repo;
}
