package it.nutrizionista.restnutrizionista.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PastoTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateAlternativaUpsertDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateItemUpsertDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateUpsertDto;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import it.nutrizionista.restnutrizionista.entity.PastoTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlimento;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlternativo;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.PastoTemplateRepository;
import jakarta.validation.Valid;

@Service
public class PastoTemplateService {
	@Autowired
	private PastoTemplateRepository repo;
	@Autowired
	private AlimentoBaseRepository alimentoBaseRepository;
	@Autowired
	private CurrentUserService currentUserService;

	@Transactional(readOnly = true)
	public List<PastoTemplateDto> listMine() {
		var me = currentUserService.getMe();
		return repo.findByCreatedBy_IdOrderByUpdatedAtDesc(me.getId()).stream()
				.map(DtoMapper::toPastoTemplateDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public PastoTemplateDto create(@Valid PastoTemplateUpsertDto req) {
		var me = currentUserService.getMe();

		PastoTemplate t = new PastoTemplate();
		t.setNome(req.getNome().trim());
		t.setDescrizione(normalizeDescrizione(req.getDescrizione()));
		t.setCreatedBy(me);
		applyItems(t, req.getAlimenti());

		return DtoMapper.toPastoTemplateDto(repo.save(t));
	}

	@Transactional
	public PastoTemplateDto update(Long id, @Valid PastoTemplateUpsertDto req) {
		var me = currentUserService.getMe();
		PastoTemplate t = repo.findByIdWithFullTree(id)
				.orElseThrow(() -> new NotFoundException("Template pasto non trovato"));
		if (t.getCreatedBy() == null || !t.getCreatedBy().getId().equals(me.getId())) {
			throw new ForbiddenException("NON AUTORIZZATO: template pasto non accessibile");
		}

		t.setNome(req.getNome().trim());
		t.setDescrizione(normalizeDescrizione(req.getDescrizione()));
		applyItems(t, req.getAlimenti());

		return DtoMapper.toPastoTemplateDto(repo.save(t));
	}

	@Transactional
	public void delete(Long id) {
		var me = currentUserService.getMe();
		PastoTemplate t = repo.findById(id).orElseThrow(() -> new NotFoundException("Template pasto non trovato"));
		if (t.getCreatedBy() == null || !t.getCreatedBy().getId().equals(me.getId())) {
			throw new ForbiddenException("NON AUTORIZZATO: template pasto non accessibile");
		}
		repo.delete(t);
	}

	private void applyItems(PastoTemplate template, List<PastoTemplateItemUpsertDto> items) {
		template.getAlimenti().clear();
		if (items == null || items.isEmpty())
			return;

		List<PastoTemplateAlimento> rows = new ArrayList<>();
		for (var it : items) {
			if (it == null)
				continue;
			if (it.getAlimentoId() == null)
				continue;

			var alimento = alimentoBaseRepository.findById(it.getAlimentoId())
					.orElseThrow(() -> new NotFoundException("Alimento non trovato: " + it.getAlimentoId()));

			PastoTemplateAlimento row = new PastoTemplateAlimento();
			row.setTemplate(template);
			row.setAlimento(alimento);
			row.setQuantita(it.getQuantita() != null ? it.getQuantita() : 100d);
			String nomeCustom = normalizeNomeCustom(it.getNomeCustom(), alimento.getNome());
			row.setNomeCustom(nomeCustom);
			row.setAlternative(new java.util.LinkedHashSet<>());

			List<PastoTemplateAlternativo> altRows = new ArrayList<>();
			List<PastoTemplateAlternativaUpsertDto> alternatives = it.getAlternative();
			if (alternatives != null && !alternatives.isEmpty()) {
				for (int i = 0; i < alternatives.size(); i++) {
					PastoTemplateAlternativaUpsertDto alt = alternatives.get(i);
					if (alt == null)
						continue;
					if (alt.getAlimentoAlternativoId() == null)
						continue;
					if (alt.getAlimentoAlternativoId().equals(alimento.getId())) {
						throw new BadRequestException(
								"L'alimento alternativo deve essere diverso dall'alimento principale");
					}

					var alimentoAlt = alimentoBaseRepository.findById(alt.getAlimentoAlternativoId())
							.orElseThrow(() -> new NotFoundException(
									"Alimento alternativo non trovato: " + alt.getAlimentoAlternativoId()));

					PastoTemplateAlternativo entity = new PastoTemplateAlternativo();
					entity.setTemplateAlimento(row);
					entity.setAlimentoAlternativo(alimentoAlt);
					entity.setQuantita(alt.getQuantita() != null ? alt.getQuantita() : 100);
					entity.setPriorita(
							alt.getPriorita() != null && alt.getPriorita() > 0 ? alt.getPriorita() : (i + 1));
					entity.setMode(alt.getMode() != null ? alt.getMode() : AlternativeMode.CALORIE);
					entity.setManual(alt.getManual() != null ? alt.getManual() : Boolean.TRUE);
					entity.setNote(normalizeDescrizione(alt.getNote()));
					entity.setNomeCustom(normalizeNomeCustom(alt.getNomeCustom(), alimentoAlt.getNome()));
					altRows.add(entity);
				}
			}

			row.getAlternative().addAll(altRows);
			rows.add(row);
		}

		template.getAlimenti().addAll(rows);
	}

	private String normalizeDescrizione(String descrizione) {
		if (descrizione == null)
			return null;
		String trimmed = descrizione.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeNomeCustom(String nomeCustom, String baseNome) {
		if (nomeCustom == null)
			return null;
		String trimmed = nomeCustom.trim();
		if (trimmed.isEmpty())
			return null;
		if (baseNome != null && trimmed.equalsIgnoreCase(baseNome.trim()))
			return null;
		return trimmed;
	}
}
