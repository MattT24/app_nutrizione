package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import jakarta.validation.Valid;

@Service
public class ClienteService {

	@Autowired private ClienteRepository repo;
	@Autowired private UtenteRepository repoUtente;

	@Transactional
	public ClienteDto create(@Valid ClienteFormDto form) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = repoUtente.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		Cliente c = DtoMapper.toCliente(form);
		c.setNutrizionista(u);
		
		return DtoMapper.toClienteDtoLight(repo.save(c));
	}


	@Transactional
	public ClienteDto update(@Valid ClienteFormDto form) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = repoUtente.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		if (form.getId() == null) throw new RuntimeException("Id cliente obbligatorio per update");
		Cliente c = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
		if (!c.getNutrizionista().getId().equals(u.getId())) throw new RuntimeException("Non autorizzato");
		DtoMapper.updateClienteFromForm(c, form);
		return DtoMapper.toClienteDto(repo.save(c));
	}

	@Transactional
    public void delete(Long id) { repo.deleteById(id); }
	
	public void deleteMyCliente(Long id) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = repoUtente.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
	    if (id == null) throw new RuntimeException("Id cliente obbligatorio per il delete");
	    Cliente c = repo.findById(id)
	    		.orElseThrow(() -> new RuntimeException("Cliente non trovato"));
	    if(!u.getId().equals(c.getNutrizionista().getId())) throw new RuntimeException("L'utente non possiede il cliente");
		repo.deleteById(id);
	}

	@Transactional(readOnly = true)
	public PageResponse<ClienteDto> allMyClienti( Pageable pageable) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    Utente u = repoUtente.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		return PageResponse.from(repo.findByNutrizionistaId(u.getId(),pageable).map(DtoMapper::toClienteDtoLight));
	}

	@Transactional(readOnly = true)
	public ClienteDto getById(Long id) {
		return  repo.findById(id).map(DtoMapper::toClienteDtoLight).orElseThrow(()-> new RuntimeException("Cliente non trovato"));
	}
	
	@Transactional(readOnly = true)
	public ClienteDto getByNome(@Valid String nome) {
		Cliente c = repo.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
		return DtoMapper.toClienteDtoLight(c);
	}
	@Transactional(readOnly = true)
	public ClienteDto getByCognome(@Valid String cognome) {
		Cliente c = repo.findByCognome(cognome)
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
		return DtoMapper.toClienteDtoLight(c);
	}
	
	@Transactional(readOnly = true)
	public ClienteDto dettaglio(Long id) {
		return repo.findById(id).map(DtoMapper::toClienteDto).orElseThrow(()-> new RuntimeException("Cliente non trovato"));
	}
	//manca cliente Fabbisogno, da studiare un attimo
}
