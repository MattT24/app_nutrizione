package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Micro;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.MicroRepository;
import jakarta.validation.Valid;

@Service
public class AlimentoBaseService {

	@Autowired private AlimentoBaseRepository repo;
	@Autowired private  MicroRepository microRepository;

	@Transactional
	public AlimentoBaseDto create(@Valid AlimentoBaseFormDto form) {
	    Map<Long, Micro> microCatalogo = loadMicroCatalogo();
	    AlimentoBase a = DtoMapper.toAlimentoBase(form, microCatalogo);
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}


	@Transactional
	public AlimentoBaseDto update(@Valid AlimentoBaseFormDto form) {
	    if (form.getId() == null) {
	        throw new RuntimeException("Id Alimento obbligatorio per update");
	    }
	    AlimentoBase a = repo.findById(form.getId())
	            .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
	    Map<Long, Micro> microCatalogo = loadMicroCatalogo();
	    DtoMapper.updateAlimentoBaseFromForm(a, form, microCatalogo);
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}

	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	@Transactional(readOnly = true)
	public PageResponse<AlimentoBaseDto> listAll(Pageable pageable) {
		return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toAlimentoBaseDtoLight));
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
		return DtoMapper.toAlimentoBaseDtoLight(a);
	}
	// Aggiungi questo metodo nel Service
	@Transactional(readOnly = true)
	public List<AlimentoBaseDto> search(String query) {
	    String normalizedQuery = query == null ? "" : query.trim();
	    List<AlimentoBase> list = repo.searchByNomeRanked(normalizedQuery);
	    // Ritorna la lista mappata (Light va bene per le select)
	    return list.stream()
	               .map(DtoMapper::toAlimentoBaseDtoLight)
	               .collect(Collectors.toList());
	}

	private Map<Long, Micro> loadMicroCatalogo() {
	    return microRepository.findAll()
	            .stream()
	            .collect(Collectors.toMap(
	                Micro::getId,
	                Function.identity()
	            ));
	}
}
