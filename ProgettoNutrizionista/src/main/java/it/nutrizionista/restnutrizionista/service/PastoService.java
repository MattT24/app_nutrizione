package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.dto.PastoFormDto;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import jakarta.validation.Valid;

@Service
public class PastoService {

	@Autowired private PastoRepository repo;
	@Autowired private SchedaRepository schedaRepo;
	@Autowired private UtenteRepository utenteRepo;

	
	@Transactional
    public PastoDto create(@Valid PastoFormDto form) {
        if (form.getScheda().getId() == null) throw new RuntimeException("ID Scheda obbligatorio");     
        Scheda scheda = schedaRepo.findById(form.getScheda().getId())
             .orElseThrow(() -> new RuntimeException("Scheda non trovata"));
        Pasto p = new Pasto();
        p.setNome(form.getNome());
        p.setOrarioInizio(form.getOrarioInizio());
        p.setOrarioFine(form.getOrarioFine());
        p.setScheda(scheda);
        return DtoMapper.toPastoDtoLight(repo.save(p));
    }


	@Transactional
    public PastoDto update(@Valid PastoFormDto form) {
        if (form.getId() == null) throw new RuntimeException("Id Pasto obbligatorio");
        Pasto p = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
        p.setNome(form.getNome());
        p.setOrarioInizio(form.getOrarioInizio());
        p.setOrarioFine(form.getOrarioFine());
        return DtoMapper.toPastoDtoLight(repo.save(p));
    }
	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	@Transactional(readOnly = true)
    public PageResponse<PastoDto> listAllMyPasti(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utente nutrizionista = utenteRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
        return PageResponse.from(
            repo.findByNutrizionista_Id(nutrizionista.getId(), pageable)
                .map(DtoMapper::toPastoDtoLight) //restituirà solo i nomi dei pasti e non gli alimenti all'interno
        );
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


