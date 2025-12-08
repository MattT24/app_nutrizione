package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import jakarta.validation.Valid;

@Service
public class AlimentoBaseService {

	@Autowired private AlimentoBaseRepository repo;

	@Transactional
	public AlimentoBaseDto create(@Valid AlimentoBaseFormDto form) {
		AlimentoBase a = DtoMapper.toAlimentoBase(form);
		return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}


	@Transactional
	public AlimentoBaseDto update(@Valid AlimentoBaseFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id Alimento obbligatorio per update");
		AlimentoBase a = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
		DtoMapper.updateAlimentoBaseFromForm(a, form);
		return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}

	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	@Transactional(readOnly = true)
	public PageResponse<AlimentoBaseDto> listAll(Pageable pageable) {
		return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toAlimentoBaseDto));
	}

	@Transactional(readOnly = true)
	public AlimentoBaseDto getById(Long id) {
		return  repo.findById(id).map(DtoMapper::toAlimentoBaseDtoLight).orElseThrow(()-> new RuntimeException("Alimento non trovato"));
	}
	
	@Transactional(readOnly = true)
	public AlimentoBaseDto dettaglio(Long id) {
		return repo.findById(id).map(DtoMapper::toAlimentoBaseDto).orElseThrow(()-> new RuntimeException("Alimento non trovato"));
	}
	@Transactional(readOnly = true)
	public AlimentoBaseDto dettaglioMacro(Long id) {
		return repo.findById(id).map(DtoMapper::toAlimentoBaseDtoMacro).orElseThrow(()-> new RuntimeException("Alimento non trovato"));
	}
	
	@Transactional(readOnly = true)
	public AlimentoBaseDto getByNome(@Valid String nome) {
		AlimentoBase a = repo.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
		return DtoMapper.toAlimentoBaseDto(a);
	}

}
