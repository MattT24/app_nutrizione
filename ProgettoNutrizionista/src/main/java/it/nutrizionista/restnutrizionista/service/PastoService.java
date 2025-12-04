package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.dto.PastoFormDto;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import jakarta.validation.Valid;

@Service
public class PastoService {

	@Autowired private PastoRepository repo;

	@Transactional
	public PastoDto create(@Valid PastoFormDto form) {
		Pasto p = new Pasto();
		return DtoMapper.toPastoDtoLight(repo.save(p));
	}


	@Transactional
	public PastoDto update(@Valid PastoFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id Pasto obbligatorio per update");
		Pasto p = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
		return DtoMapper.toPastoDtoLight(repo.save(p));
	}

	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	@Transactional(readOnly = true)
	public PageResponse<PastoDto> listAll(Pageable pageable) {
		return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toPastoDtoLight));
	}

	@Transactional(readOnly = true)
	public PastoDto getById(Long id) {
		return  repo.findById(id).map(DtoMapper::toPastoDtoLight).orElseThrow(()-> new RuntimeException("Pasto non trovato"));
	}
	
	@Transactional(readOnly = true)
	public PastoDto dettaglio(Long id) {
		return repo.findById(id).map(DtoMapper::toPastoDto).orElseThrow(()-> new RuntimeException("Pasto non trovato"));
	}


}


