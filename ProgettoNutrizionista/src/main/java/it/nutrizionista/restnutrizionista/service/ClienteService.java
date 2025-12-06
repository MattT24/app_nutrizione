package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.entity.Cliente;

import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import jakarta.validation.Valid;

@Service
public class ClienteService {

	@Autowired private ClienteRepository repo;

	@Transactional
	public ClienteDto create(@Valid ClienteFormDto form) {
		Cliente c = new Cliente();
		return DtoMapper.toClienteDto(repo.save(c));
	}


	@Transactional
	public ClienteDto update(@Valid ClienteFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id cliente obbligatorio per update");
		Cliente c = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
		return DtoMapper.toClienteDto(repo.save(c));
	}

	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	@Transactional(readOnly = true)
	public PageResponse<ClienteDto> listAll(Pageable pageable) {
		return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toClienteDto));
	}

	@Transactional(readOnly = true)
	public ClienteDto getById(Long id) {
		return  repo.findById(id).map(DtoMapper::toClienteDtoLight).orElseThrow(()-> new RuntimeException("Cliente non trovato"));
	}
	
	@Transactional(readOnly = true)
	public ClienteDto getByNome(@Valid String nome) {
		Cliente c = repo.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
		return DtoMapper.toClienteDto(c);
	}
	@Transactional(readOnly = true)
	public ClienteDto getByCognome(@Valid String cognome) {
		Cliente c = repo.findByCognome(cognome)
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
		return DtoMapper.toClienteDto(repo.save(c));
	}
	
	@Transactional(readOnly = true)
	public ClienteDto dettaglio(Long id) {
		return repo.findById(id).map(DtoMapper::toClienteDto).orElseThrow(()-> new RuntimeException("Cliente non trovato"));
	}
	//manca cliente Fabbisogno, da studiare un attimo
}
