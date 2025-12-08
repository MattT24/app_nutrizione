package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.nutrizionista.restnutrizionista.dto.MicroDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.Micro;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.MicroRepository;

@Service
public class MicroService {

	@Autowired private MicroRepository repo;
	
	@Transactional(readOnly = true)
	public PageResponse<MicroDto> listAll(Pageable pageable) {
		return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toMicroDto));
	}

	public MicroDto getByNome(String nome) {
		Micro m = repo.findByNome(nome).orElseThrow(() -> new RuntimeException("Micronutriente non trovato"));
		return DtoMapper.toMicroDto(m);
	}
}
