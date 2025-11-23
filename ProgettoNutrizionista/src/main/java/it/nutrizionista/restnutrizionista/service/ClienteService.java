package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.ClienteRepository;

@Service
public class ClienteService {

	@Autowired private ClienteRepository repo;
	
	
}
