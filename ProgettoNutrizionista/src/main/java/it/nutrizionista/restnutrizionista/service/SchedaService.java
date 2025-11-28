package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import jakarta.validation.Valid;

@Service
public class SchedaService {

	@Autowired private SchedaRepository repo;

	public SchedaDto create(@Valid SchedaFormDto form) {
		Scheda s = new Scheda();
		
		return DtoMapper.toSchedaDto(repo.save(s));
	}
}
