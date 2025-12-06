package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoDaEvitareFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoDaEvitareRepository;
import jakarta.validation.Valid;

@Service
public class AlimentoDaEvitareService {

	@Autowired private AlimentoDaEvitareRepository repo;

	@Transactional
	public AlimentoDaEvitareDto create(@Valid AlimentoDaEvitareFormDto form) {
		AlimentoDaEvitare a = new AlimentoDaEvitare();
		return DtoMapper.toAlimentoDaEvitareDtoLight(repo.save(a));
	}

	@Transactional
	public AlimentoDaEvitareDto update(@Valid AlimentoDaEvitareFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id AlimentoDaEvitare obbligatorio per update");
		AlimentoDaEvitare a = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("AlimentoDaEvitare non trovato"));
		return DtoMapper.toAlimentoDaEvitareDtoLight(repo.save(a));
	}
	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	@Transactional(readOnly = true)
	public PageResponse<AlimentoDaEvitareDto> listAll(Pageable pageable) {
		return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toAlimentoDaEvitareDtoLight));
	}

	@Transactional(readOnly = true)
	public AlimentoDaEvitareDto getById(Long id) {
		return  repo.findById(id).map(DtoMapper::toAlimentoDaEvitareDtoLight).orElseThrow(()-> new RuntimeException("AlimentoDaEvitare non trovato"));
	}
	
	@Transactional(readOnly = true)
	public AlimentoDaEvitareDto dettaglio(Long id) {
		return repo.findById(id).map(DtoMapper::toAlimentoDaEvitareDto).orElseThrow(()-> new RuntimeException("AlimentoDaEvitare non trovato"));
	}
	
	@Transactional(readOnly = true)
	public AlimentoDaEvitareDto getByNome(@Valid String nome) {
		AlimentoDaEvitare a = repo.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
		return DtoMapper.toAlimentoDaEvitareDto(a);
	}
}
