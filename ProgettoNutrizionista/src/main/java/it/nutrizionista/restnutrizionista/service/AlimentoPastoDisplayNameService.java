package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoNomeOverrideRepository;

@Service
public class AlimentoPastoDisplayNameService {
	@Autowired private OwnershipValidator ownershipValidator;
	@Autowired private AlimentoPastoNomeOverrideRepository repo;

	@Transactional
	public AlimentoPastoDto setDisplayName(Long schedaId, Long alimentoPastoId, String nome) {
		var ap = ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
		if (ap.getPasto() == null || ap.getPasto().getScheda() == null || !ap.getPasto().getScheda().getId().equals(schedaId)) {
			throw new ForbiddenException("NON AUTORIZZATO: alimento pasto non appartiene alla scheda");
		}

		AlimentoPastoNomeOverride ov = repo.findByAlimentoPasto_Id(alimentoPastoId).orElseGet(() -> {
			AlimentoPastoNomeOverride created = new AlimentoPastoNomeOverride();
			created.setAlimentoPasto(ap);
			return created;
		});
		ov.setNomeCustom(nome);
		AlimentoPastoNomeOverride saved = repo.save(ov);
		ap.setNomeOverride(saved);
		return DtoMapper.toAlimentoPastoDtoFull(ap);
	}

	@Transactional
	public AlimentoPastoDto deleteDisplayName(Long schedaId, Long alimentoPastoId) {
		var ap = ownershipValidator.getOwnedAlimentoPasto(alimentoPastoId);
		if (ap.getPasto() == null || ap.getPasto().getScheda() == null || !ap.getPasto().getScheda().getId().equals(schedaId)) {
			throw new ForbiddenException("NON AUTORIZZATO: alimento pasto non appartiene alla scheda");
		}
		if (repo.existsByAlimentoPasto_Id(alimentoPastoId)) {
			repo.deleteByAlimentoPasto_Id(alimentoPastoId);
		}
		ap.setNomeOverride(null);
		return DtoMapper.toAlimentoPastoDtoFull(ap);
	}
}

