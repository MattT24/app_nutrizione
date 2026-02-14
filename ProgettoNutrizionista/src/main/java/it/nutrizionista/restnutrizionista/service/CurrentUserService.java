package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;

@Service
public class CurrentUserService {

	@Autowired
	private UtenteRepository repoUtente;

	public Utente getMe() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return repoUtente.findByEmail(email)
				.orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));
	}
}
