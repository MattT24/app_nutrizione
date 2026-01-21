package it.nutrizionista.restnutrizionista.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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

	
	private Utente getMe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return repoUtente.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
    }
	
	@Transactional
	public ClienteDto create(@Valid ClienteFormDto form) {
		Utente u = getMe();
		//controllo se è già presente un cliente con quel CF
		if(repo.existsByCodiceFiscale(form.getCodiceFiscale())) {
            throw new RuntimeException("Cliente già esistente (CF duplicato)");
       }
		Cliente c = DtoMapper.toCliente(form);
		c.setNutrizionista(u);
		return DtoMapper.toClienteDtoLight(repo.save(c));
	}


	@Transactional
	public ClienteDto update(@Valid ClienteFormDto form) {
	    Utente u = getMe();
		if (form.getId() == null) throw new RuntimeException("Id cliente obbligatorio per update");
		Cliente c = repo.findByIdAndNutrizionistaId(form.getId(), u.getId())
                .orElseThrow(() -> new RuntimeException("Cliente non trovato o non autorizzato"));
		DtoMapper.updateClienteFromForm(c, form);
		return DtoMapper.toClienteDto(repo.save(c));
	}

	public void deleteMyCliente(Long id) {
	    Utente me = getMe();
	    if (id == null) throw new RuntimeException("Id cliente obbligatorio per il delete");
        Cliente c = repo.findByIdAndNutrizionistaId(id, me.getId())
                .orElseThrow(() -> new RuntimeException("Cliente non trovato o non autorizzato"));
		repo.delete(c);
	}

	@Transactional(readOnly = true)
	public PageResponse<ClienteDto> allMyClienti( Pageable pageable) {
		Utente u = getMe();
	    int maxSize = 12;
	    if (pageable.getPageSize() > maxSize) {
	        pageable = PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
	    }		
		return PageResponse.from(repo.findByNutrizionistaId(u.getId(),pageable).map(DtoMapper::toClienteDtoLight));
	}

	@Transactional(readOnly = true)
    public ClienteDto getById(Long id) {
        Utente me = getMe();
        return repo.findByIdAndNutrizionistaId(id, me.getId())
                .map(DtoMapper::toClienteDtoLight)
                .orElseThrow(() -> new RuntimeException("Cliente non trovato o non autorizzato"));
    }
	
	@Transactional(readOnly = true)
    public List<ClienteDto> findByNome(String nome) {
        Utente me = getMe();
        return repo.findByNutrizionistaIdAndNomeContainingIgnoreCase(me.getId(), nome)
                .stream().map(DtoMapper::toClienteDtoLight).toList();
    }
	@Transactional(readOnly = true)
	public List<ClienteDto> findByCognome(@Valid String cognome) {
        Utente me = getMe();
        return repo.findByNutrizionistaIdAndCognomeContainingIgnoreCase(me.getId(), cognome)
                .stream().map(DtoMapper::toClienteDtoLight).toList();
	}
	
	@Transactional(readOnly = true)
    public ClienteDto dettaglio(Long id) {
        Utente me = getMe();
        // Qui usi il mapper completo (con misurazioni ecc)
        return repo.findByIdAndNutrizionistaId(id, me.getId())
                .map(DtoMapper::toClienteDto) 
                .orElseThrow(() -> new RuntimeException("Cliente non trovato o non autorizzato"));
    }
	//manca cliente Fabbisogno, da studiare un attimo
}
