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
	@Autowired private LimitazioneTrattamentoValidator limitazioneValidator;
	@Autowired private SchedaFascicoloSync fascicoloSync;

	@Transactional
	public PastoDto create(@Valid MealCreateRequest req) {
		var scheda = ownershipValidator.getOwnedScheda(req.getSchedaId());
		limitazioneValidator.assertNonLimitato(scheda.getCliente()); // A5.3
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

		// Gestione giorno per schede settimanali
		if (req.getGiorno() != null && !req.getGiorno().isBlank()) {
			try {
				p.setGiorno(it.nutrizionista.restnutrizionista.entity.GiornoSettimana.valueOf(req.getGiorno()));
			} catch (IllegalArgumentException e) {
				// Ignore invalid giorno
			}
		}

		Pasto salvato = repo.save(p);
		fascicoloSync.sincronizza(scheda.getCliente().getId(), scheda.getId());
		return DtoMapper.toPastoDtoLight(salvato);
	}

	@Transactional
	public PastoDto update(Long id, @Valid MealUpdateRequest req) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		limitazioneValidator.assertNonLimitato(p.getScheda().getCliente()); // A5.3
		if (Boolean.FALSE.equals(p.getEliminabile())) {
			throw new ForbiddenException("NON AUTORIZZATO: non puoi modificare un pasto default");
		}
		p.setNome(req.getNome());
		p.setDescrizione(req.getDescrizione());
		if (req.getOrdineVisualizzazione() != null) p.setOrdineVisualizzazione(req.getOrdineVisualizzazione());
		Pasto salvato = repo.save(p);
		fascicoloSync.sincronizza(p.getScheda().getCliente().getId(), p.getScheda().getId());
		return DtoMapper.toPastoDtoLight(salvato);
	}

	@Transactional
	public void delete(Long id) {
		Pasto p = ownershipValidator.getOwnedPasto(id);
		limitazioneValidator.assertNonLimitato(p.getScheda().getCliente()); // A5.3
		if (Boolean.FALSE.equals(p.getEliminabile())) {
			throw new ForbiddenException("NON AUTORIZZATO: non puoi eliminare un pasto default");
		}
		Long clienteId = p.getScheda().getCliente().getId();
		Long schedaId = p.getScheda().getId();
		repo.delete(p);
		fascicoloSync.sincronizza(clienteId, schedaId);
	}
}

