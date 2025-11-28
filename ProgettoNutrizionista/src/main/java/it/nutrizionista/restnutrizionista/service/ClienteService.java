package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import jakarta.validation.Valid;

@Service
public class ClienteService {

	@Autowired private ClienteRepository repo;
	@Autowired private UtenteRepository utenteRepo;

	@Transactional
	public ClienteDto create(@Valid ClienteFormDto form) {
		Cliente c = new Cliente();
		return DtoMapper.toClienteDto(repo.save(c));
	}

	@Transactional(readOnly = true)
	public ClienteDto dettaglio(Long id) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = utenteRepo.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
	    
		return null;
	}

	@Transactional
	public ClienteDto update(@Valid ClienteFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id gruppo obbligatorio per update");
		Cliente c = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Gruppo non trovato"));
		return DtoMapper.toClienteDto(repo.save(c));
	}

	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	
	
}
