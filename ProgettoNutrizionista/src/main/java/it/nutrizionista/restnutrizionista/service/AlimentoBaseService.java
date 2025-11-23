package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;

@Service
public class AlimentoBaseService {

	@Autowired private AlimentoBaseRepository repo;
	
}
