package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.MacroRepository;

@Service
public class MacroService {

	@Autowired private MacroRepository repo;
	
	@Transactional(readOnly = true)
	public PageResponse<MacroDto> listAll(Pageable pageable) {
		return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toMacroDto));
	}
}
