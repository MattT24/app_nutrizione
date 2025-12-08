package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.MacroRepository;

@Service
public class MacroService {

	@Autowired private MacroRepository repo;
	
	@Transactional(readOnly = true)
	public MacroDto getByAlimento(Long id) {
		return repo.findByAlimento_Id(id).map(DtoMapper::toMacroDto).orElseThrow(() -> new RuntimeException("Macronutrienti non trovati"));
	}
}
