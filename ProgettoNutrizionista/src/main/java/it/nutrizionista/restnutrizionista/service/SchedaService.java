package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import jakarta.validation.Valid;

@Service
public class SchedaService {

	@Autowired private SchedaRepository repo;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private ClienteRepository repoCliente;

	
	@Transactional
	public SchedaDto create(@Valid SchedaFormDto form) {
		if (form.getId() != null) throw new RuntimeException("Id non richiesto per create");
		Scheda s = new Scheda();
		s.setAttiva(true);	
		s.setCliente(form.getCliente());
		return DtoMapper.toSchedaDto(repo.save(s));
	}
	
	@Transactional
	public SchedaDto update(SchedaFormDto form) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = repoUtente.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		if (form.getId() == null) throw new RuntimeException("Id scheda obbligatorio per update");
		Scheda s = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Scheda non trovata"));
		if (u.getId()!= s.getCliente().getNutrizionista().getId()) throw new RuntimeException("Non autorizzato");
		DtoMapper.updateSchedaFromForm(s, form);
		return DtoMapper.toSchedaDto(repo.save(s));
	}
	
	@Transactional
	public void delete(Long id) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = repoUtente.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
	    if (id == null) throw new RuntimeException("Id scheda obbligatorio per il delete");
	    Scheda s = repo.findById(id)
	    		.orElseThrow(() -> new RuntimeException("Scheda non trovata"));
	    if(u.getId()!= s.getCliente().getNutrizionista().getId()) throw new RuntimeException("L'utente non possiede la scheda");
		repo.deleteById(id);
	}
	
	@Transactional(readOnly = true)
	public SchedaDto getById(Long id) {
		return  repo.findById(id).map(DtoMapper::toSchedaDtoLight).orElseThrow(()-> new RuntimeException("Scheda non trovata"));
	}
	
	@Transactional(readOnly = true)
	public SchedaDto pastiByScheda(Long id) {
		return  repo.findById(id).map(DtoMapper::toSchedaDtoListaPasti).orElseThrow(()-> new RuntimeException("Scheda non trovata"));
	}
	
	@Transactional(readOnly = true)
	public List<SchedaDto> schedeByCliente(Long id) {
		Cliente c = repoCliente.findById(id).orElseThrow(()-> new RuntimeException("Cliente non trovato"));
		return c.getSchede().stream().map(DtoMapper::toSchedaDtoListaPasti).collect(Collectors.toList());
	}
	
	
	
	
}
