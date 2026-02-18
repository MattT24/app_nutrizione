package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.MealCreateRequest;
import it.nutrizionista.restnutrizionista.dto.MealUpdateRequest;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import jakarta.validation.Valid;

@Service
public class MealService {
	@Autowired private PastoRepository repo;
	@Autowired private OwnershipValidator ownershipValidator;

	@Transactional
	public PastoDto create(@Valid MealCreateRequest req) {
		var scheda = ownershipValidator.getOwnedScheda(req.getSchedaId());
		Pasto p = new Pasto();
		p.setScheda(scheda);
		p.setNome(req.getNome());
		p.setDescrizione(req.getDescrizione());
		p.setDefaultCode(null);
		p.setEliminabile(true);
		if (req.getOrdineVisualizzazione() != null) {
			p.setOrdineVisualizzazione(req.getOrdineVisualizzazione());
		} else {
			var last = repo.findTopByScheda_IdOrderByOrdineVisualizzazioneDescIdDesc(scheda.getId()).orElse(null);
			int next = (last != null && last.getOrdineVisualizzazione() != null) ? last.getOrdineVisualizzazione() + 1 : 10;
			p.setOrdineVisualizzazione(next);
		}
		return DtoMapper.toPastoDtoLight(repo.save(p));
	}

	@Transactional
	public PastoDto update(Long id, @Valid MealUpdateRequest req) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		if (Boolean.FALSE.equals(p.getEliminabile())) {
			throw new ForbiddenException("NON AUTORIZZATO: non puoi modificare un pasto default");
		}
		p.setNome(req.getNome());
		p.setDescrizione(req.getDescrizione());
		if (req.getOrdineVisualizzazione() != null) p.setOrdineVisualizzazione(req.getOrdineVisualizzazione());
		return DtoMapper.toPastoDtoLight(repo.save(p));
	}

	@Transactional
	public void delete(Long id) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		if (Boolean.FALSE.equals(p.getEliminabile())) {
			throw new ForbiddenException("NON AUTORIZZATO: non puoi eliminare un pasto default");
		}
		repo.delete(p);
	}
}

