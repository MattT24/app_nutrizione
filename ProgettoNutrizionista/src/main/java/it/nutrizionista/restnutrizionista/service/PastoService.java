package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.dto.PastoFormDto;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import jakarta.validation.Valid;

@Service
public class PastoService {

	@Autowired private PastoRepository repo;
	@Autowired private SchedaRepository schedaRepo;
	@Autowired private CurrentUserService currentUserService;
	@Autowired private OwnershipValidator ownershipValidator;

	
	@Transactional
    public PastoDto create(@Valid PastoFormDto form) {
        if (form.getScheda().getId() == null) throw new RuntimeException("ID Scheda obbligatorio");     
        Scheda scheda = ownershipValidator.getOwnedScheda(form.getScheda().getId());
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
        Pasto p = ownershipValidator.getOwnedPasto(form.getId());
        p.setNome(form.getNome());
        p.setOrarioInizio(form.getOrarioInizio());
        p.setOrarioFine(form.getOrarioFine());
        return DtoMapper.toPastoDtoLight(repo.save(p));
    }
	@Transactional
    public void delete(Long id) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		repo.delete(p);
	}

	@Transactional(readOnly = true)
    public PageResponse<PastoDto> listAllMyPasti(Pageable pageable) {
        var nutrizionista = currentUserService.getMe();
        return PageResponse.from(
            repo.findByNutrizionista_Id(nutrizionista.getId(), pageable)
                .map(DtoMapper::toPastoDtoLight) //restituirà solo i nomi dei pasti e non gli alimenti all'interno
        );
    }

	@Transactional(readOnly = true)
	public PastoDto getById(Long id) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		return DtoMapper.toPastoDtoLight(p);
	}
	
	@Transactional(readOnly = true)
	public PastoDto dettaglio(Long id) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		return DtoMapper.toPastoDto(p);
	}


}


