package it.nutrizionista.restnutrizionista.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import jakarta.validation.Valid;

@Service
public class ClienteService {

	@Autowired private ClienteRepository repo;
	@Autowired private CurrentUserService currentUserService;
	@Autowired private OwnershipValidator ownershipValidator;

	@Transactional
	public ClienteDto create(@Valid ClienteFormDto form) {
		Utente u = currentUserService.getMe();
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
		if (form.getId() == null) throw new RuntimeException("Id cliente obbligatorio per update");
		Cliente c = ownershipValidator.getOwnedCliente(form.getId());
		DtoMapper.updateClienteFromForm(c, form);
		return DtoMapper.toClienteDto(repo.save(c));
	}

	public void deleteMyCliente(Long id) {
	    if (id == null) throw new RuntimeException("Id cliente obbligatorio per il delete");
        Cliente c = ownershipValidator.getOwnedCliente(id);
		repo.delete(c);
	}

	@Transactional(readOnly = true)
	public PageResponse<ClienteDto> allMyClienti( Pageable pageable) {
		Utente u = currentUserService.getMe();
	    int maxSize = 12;
	    if (pageable.getPageSize() > maxSize) {
	        pageable = PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
	    }		
		return PageResponse.from(repo.findByNutrizionista_Id(u.getId(),pageable).map(DtoMapper::toClienteDtoLight));
	}

	@Transactional(readOnly = true)
    public ClienteDto getById(Long id) {
        Cliente c = ownershipValidator.getOwnedCliente(id);
        return DtoMapper.toClienteDtoLight(c);
    }
	
	@Transactional(readOnly = true)
    public List<ClienteDto> findByNome(String nome) {
        Utente me = currentUserService.getMe();
        return repo.findByNutrizionista_IdAndNomeContainingIgnoreCase(me.getId(), nome)
                .stream().map(DtoMapper::toClienteDtoLight).toList();
    }
	@Transactional(readOnly = true)
	public List<ClienteDto> findByCognome(@Valid String cognome) {
        Utente me = currentUserService.getMe();
        return repo.findByNutrizionista_IdAndCognomeContainingIgnoreCase(me.getId(), cognome)
                .stream().map(DtoMapper::toClienteDtoLight).toList();
	}
	
	@Transactional(readOnly = true)
    public ClienteDto dettaglio(Long id) {
        Cliente c = ownershipValidator.getOwnedCliente(id);
        return DtoMapper.toClienteDto(c);
    }
	//manca cliente Fabbisogno, da studiare un attimo
}
