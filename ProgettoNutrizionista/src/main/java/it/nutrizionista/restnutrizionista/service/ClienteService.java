package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.entity.Utente;
import jakarta.validation.Valid;

@Service
public class ClienteService {

	@Autowired private ClienteRepository repo;
	@Autowired private UtenteRepository utenteRepo;

	public ClienteDto create(@Valid ClienteFormDto form) {
		// TODO Auto-generated method stub
		return null;
	}

	public ClienteDto dettaglio(Long id) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = utenteRepo.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
	    
		return null;
	}
	
	
}
