package it.nutrizionista.restnutrizionista.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PresetObiettivoDto;
import it.nutrizionista.restnutrizionista.entity.PresetObiettivo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.PresetObiettivoRepository;

@Service
public class PresetObiettivoService {

	@Autowired
	private PresetObiettivoRepository repo;
	@Autowired
	private CurrentUserService currentUserService;

	@Transactional(readOnly = true)
	public List<PresetObiettivoDto> getAll() {
		Utente me = currentUserService.getMe();
		return repo.findByNutrizionista_IdOrderByNomeAsc(me.getId())
				.stream().map(this::toDto).toList();
	}

	@Transactional
	public PresetObiettivoDto crea(PresetObiettivoDto dto) {
		Utente me = currentUserService.getMe();
		PresetObiettivo p = new PresetObiettivo();
		p.setNutrizionista(me);
		p.setNome(dto.getNome());
		p.setPctProteine(dto.getPctProteine());
		p.setPctCarboidrati(dto.getPctCarboidrati());
		p.setPctGrassi(dto.getPctGrassi());
		p.setMoltiplicatoreTdee(dto.getMoltiplicatoreTdee() != null ? dto.getMoltiplicatoreTdee() : 1.0);
		return toDto(repo.save(p));
	}

	@Transactional
	public void delete(Long id) {
		Utente me = currentUserService.getMe();
		repo.deleteByIdAndNutrizionista_Id(id, me.getId());
	}

	private PresetObiettivoDto toDto(PresetObiettivo p) {
		PresetObiettivoDto dto = new PresetObiettivoDto();
		dto.setId(p.getId());
		dto.setNome(p.getNome());
		dto.setPctProteine(p.getPctProteine());
		dto.setPctCarboidrati(p.getPctCarboidrati());
		dto.setPctGrassi(p.getPctGrassi());
		dto.setMoltiplicatoreTdee(p.getMoltiplicatoreTdee());
		return dto;
	}
}
