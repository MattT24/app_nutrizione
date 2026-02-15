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
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import jakarta.validation.Valid;

@Service
public class PastoService {

	@Autowired private PastoRepository repo;
	@Autowired private SchedaRepository schedaRepo;
	@Autowired private CurrentUserService currentUserService;
	@Autowired private OwnershipValidator ownershipValidator;

	private boolean isDefaultMealName(String nome) {
		return "Colazione".equalsIgnoreCase(nome)
				|| "Pranzo".equalsIgnoreCase(nome)
				|| "Merenda".equalsIgnoreCase(nome)
				|| "Cena".equalsIgnoreCase(nome);
	}

	private int defaultMealOrder(String nome) {
		if ("Colazione".equalsIgnoreCase(nome)) return 1;
		if ("Pranzo".equalsIgnoreCase(nome)) return 2;
		if ("Merenda".equalsIgnoreCase(nome)) return 3;
		if ("Cena".equalsIgnoreCase(nome)) return 4;
		return 999;
	}
	
	@Transactional
    public PastoDto create(@Valid PastoFormDto form) {
        if (form.getScheda().getId() == null) throw new RuntimeException("ID Scheda obbligatorio");
        var scheda = ownershipValidator.getOwnedScheda(form.getScheda().getId());
        Pasto p = new Pasto();
        p.setNome(form.getNome());
        validateOrari(form.getOrarioInizio(), form.getOrarioFine());
        p.setOrarioInizio(form.getOrarioInizio());
        p.setOrarioFine(form.getOrarioFine());
        p.setDescrizione(form.getDescrizione());
        p.setScheda(scheda);
        
        if (isDefaultMealName(form.getNome())) {
        	if (repo.existsByScheda_IdAndDefaultCodeIgnoreCase(scheda.getId(), form.getNome())) {
        		throw new ConflictException("Pasto default già presente nella scheda");
        	}
        	p.setDefaultCode(form.getNome());
        	p.setEliminabile(false);
        	p.setOrdineVisualizzazione(defaultMealOrder(form.getNome()));
        } else {
        	p.setDefaultCode(null);
        	p.setEliminabile(true);
        	if (form.getOrdineVisualizzazione() != null) {
        		p.setOrdineVisualizzazione(form.getOrdineVisualizzazione());
        	} else {
        		var last = repo.findTopByScheda_IdOrderByOrdineVisualizzazioneDescIdDesc(scheda.getId()).orElse(null);
        		int next = (last != null && last.getOrdineVisualizzazione() != null) ? last.getOrdineVisualizzazione() + 1 : 10;
        		p.setOrdineVisualizzazione(next);
        	}
        }
        return DtoMapper.toPastoDtoLight(repo.save(p));
    }


	@Transactional
    public PastoDto update(@Valid PastoFormDto form) {
        if (form.getId() == null) throw new RuntimeException("Id Pasto obbligatorio");
        Pasto p = ownershipValidator.getOwnedPasto(form.getId());
        if (Boolean.FALSE.equals(p.getEliminabile())) {
        	if (form.getNome() != null && !p.getNome().equalsIgnoreCase(form.getNome())) {
        		throw new ForbiddenException("NON AUTORIZZATO: non puoi rinominare un pasto default");
        	}
        } else {
        	p.setNome(form.getNome());
        	p.setDescrizione(form.getDescrizione());
        	if (form.getOrdineVisualizzazione() != null) p.setOrdineVisualizzazione(form.getOrdineVisualizzazione());
        }
        validateOrari(form.getOrarioInizio(), form.getOrarioFine());
        p.setOrarioInizio(form.getOrarioInizio());
        p.setOrarioFine(form.getOrarioFine());
        return DtoMapper.toPastoDtoLight(repo.save(p));
    }
	
	@Transactional
	public PastoDto updateOrari(Long pastoId, java.time.LocalTime orarioInizio, java.time.LocalTime orarioFine) {
		if (pastoId == null) throw new RuntimeException("Id Pasto obbligatorio");
		Pasto p = ownershipValidator.getOwnedPasto(pastoId);
		validateOrari(orarioInizio, orarioFine);
		p.setOrarioInizio(orarioInizio);
		p.setOrarioFine(orarioFine);
		return DtoMapper.toPastoDtoLight(repo.save(p));
	}
	
	private void validateOrari(java.time.LocalTime orarioInizio, java.time.LocalTime orarioFine) {
		if (orarioInizio == null && orarioFine == null) return;
		if (orarioInizio == null || orarioFine == null) {
			throw new BadRequestException("Orario inizio e fine devono essere entrambi valorizzati");
		}
		if (!orarioInizio.isBefore(orarioFine)) {
			throw new BadRequestException("Orario non valido: orarioInizio deve essere < orarioFine");
		}
	}
	@Transactional
    public void delete(Long id) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		if (Boolean.FALSE.equals(p.getEliminabile())) {
			throw new ForbiddenException("NON AUTORIZZATO: non puoi eliminare un pasto default");
		}
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


