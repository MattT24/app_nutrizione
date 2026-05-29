package it.nutrizionista.restnutrizionista.service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplateCreateDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplatePatchDto;
import it.nutrizionista.restnutrizionista.dto.PastoSchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoSchedaTemplateFormDto;
import it.nutrizionista.restnutrizionista.dto.ReorderDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.GiornoSettimana;
import it.nutrizionista.restnutrizionista.entity.PastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoSchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.PastoSchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaTemplateRepository;
import jakarta.validation.Valid;

@Service
public class PastoSchedaTemplateService {

	@Autowired private SchedaTemplateRepository templateRepo;
	@Autowired private PastoSchedaTemplateRepository pastoRepo;
	@Autowired private AlimentoPastoSchedaTemplateRepository alimentoRepo;
	@Autowired private AlimentoBaseRepository alimentoBaseRepo;
	@Autowired private CurrentUserService currentUserService;

	// ═══════════════════════════════════════════
	// PASTO CRUD
	// ═══════════════════════════════════════════

	@Transactional
	public PastoSchedaTemplateDto createPasto(Long templateId, @Valid PastoSchedaTemplateFormDto dto) {
		SchedaTemplate template = getOwnedTemplate(templateId);

		int maxOrdine = template.getPasti().stream()
				.mapToInt(p -> p.getOrdineVisualizzazione() != null ? p.getOrdineVisualizzazione() : 0)
				.max().orElse(-1);

		PastoSchedaTemplate pasto = new PastoSchedaTemplate();
		pasto.setSchedaTemplate(template);
		pasto.setNome(dto.nome().trim());
		pasto.setDescrizione(normalizeString(dto.descrizione()));
		pasto.setGiorno(parseGiorno(dto.giorno()));
		pasto.setOrdineVisualizzazione(maxOrdine + 1);
		pasto.setOrarioInizio(parseTime(dto.orarioInizio()));
		pasto.setOrarioFine(parseTime(dto.orarioFine()));

		return DtoMapper.toPastoSchedaTemplateDto(pastoRepo.save(pasto));
	}

	@Transactional
	public PastoSchedaTemplateDto updatePasto(Long templateId, Long pastoId, @Valid PastoSchedaTemplateFormDto dto) {
		getOwnedTemplate(templateId);
		PastoSchedaTemplate pasto = getOwnedPasto(pastoId);
		checkPastoBelongsToTemplate(pasto, templateId);

		pasto.setNome(dto.nome().trim());
		pasto.setDescrizione(normalizeString(dto.descrizione()));
		pasto.setGiorno(parseGiorno(dto.giorno()));
		pasto.setOrarioInizio(parseTime(dto.orarioInizio()));
		pasto.setOrarioFine(parseTime(dto.orarioFine()));

		return DtoMapper.toPastoSchedaTemplateDto(pastoRepo.save(pasto));
	}

	@Transactional
	public void deletePasto(Long templateId, Long pastoId) {
		getOwnedTemplate(templateId);
		PastoSchedaTemplate pasto = getOwnedPasto(pastoId);
		checkPastoBelongsToTemplate(pasto, templateId);
		pastoRepo.delete(pasto);
	}

	@Transactional
	public void reorderPasti(Long templateId, @Valid ReorderDto dto) {
		getOwnedTemplate(templateId);
		List<Long> ids = dto.ids();
		if (ids == null || ids.isEmpty()) return;

		List<PastoSchedaTemplate> pasti = pastoRepo.findBySchedaTemplate_IdOrderByOrdineVisualizzazioneAsc(templateId);

		for (int i = 0; i < ids.size(); i++) {
			Long targetId = ids.get(i);
			for (PastoSchedaTemplate p : pasti) {
				if (p.getId().equals(targetId)) {
					p.setOrdineVisualizzazione(i);
					break;
				}
			}
		}
		pastoRepo.saveAll(pasti);
	}

	// ═══════════════════════════════════════════
	// ALIMENTO CRUD
	// ═══════════════════════════════════════════

	@Transactional
	public AlimentoPastoSchedaTemplateDto addAlimento(Long templateId, Long pastoId,
			@Valid AlimentoPastoSchedaTemplateCreateDto dto) {
		getOwnedTemplate(templateId);
		PastoSchedaTemplate pasto = getOwnedPasto(pastoId);
		checkPastoBelongsToTemplate(pasto, templateId);

		AlimentoBase alimento = alimentoBaseRepo.findById(dto.alimentoId())
				.orElseThrow(() -> new NotFoundException("Alimento non trovato: " + dto.alimentoId()));

		int maxOrdine = pasto.getAlimenti().stream()
				.mapToInt(AlimentoPastoSchedaTemplate::getOrdine)
				.max().orElse(-1);

		AlimentoPastoSchedaTemplate apt = new AlimentoPastoSchedaTemplate();
		apt.setPastoSchedaTemplate(pasto);
		apt.setAlimento(alimento);
		apt.setQuantita(Math.max(1, dto.quantita()));
		apt.setNomeCustom(normalizeNomeCustom(dto.nomeCustom(), alimento.getNome()));
		apt.setOrdine(maxOrdine + 1);

		return DtoMapper.toAlimentoPastoSchedaTemplateDto(alimentoRepo.save(apt));
	}

	@Transactional
	public AlimentoPastoSchedaTemplateDto updateAlimento(Long templateId, Long aptId,
			@Valid AlimentoPastoSchedaTemplatePatchDto dto) {
		getOwnedTemplate(templateId);
		AlimentoPastoSchedaTemplate apt = getOwnedAlimento(aptId);
		checkAlimentoBelongsToTemplate(apt, templateId);

		apt.setQuantita(Math.max(1, dto.quantita()));
		if (dto.nomeCustom() != null) {
			String baseName = apt.getAlimento() != null ? apt.getAlimento().getNome() : null;
			apt.setNomeCustom(normalizeNomeCustom(dto.nomeCustom(), baseName));
		}

		return DtoMapper.toAlimentoPastoSchedaTemplateDto(alimentoRepo.save(apt));
	}

	@Transactional
	public void deleteAlimento(Long templateId, Long aptId) {
		getOwnedTemplate(templateId);
		AlimentoPastoSchedaTemplate apt = getOwnedAlimento(aptId);
		checkAlimentoBelongsToTemplate(apt, templateId);
		alimentoRepo.delete(apt);
	}

	@Transactional
	public void reorderAlimenti(Long templateId, Long pastoId, @Valid ReorderDto dto) {
		getOwnedTemplate(templateId);
		PastoSchedaTemplate pasto = getOwnedPasto(pastoId);
		checkPastoBelongsToTemplate(pasto, templateId);

		List<Long> ids = dto.ids();
		if (ids == null || ids.isEmpty()) return;

		List<AlimentoPastoSchedaTemplate> alimenti =
				alimentoRepo.findByPastoSchedaTemplate_IdOrderByOrdineAsc(pastoId);

		for (int i = 0; i < ids.size(); i++) {
			Long targetId = ids.get(i);
			for (AlimentoPastoSchedaTemplate a : alimenti) {
				if (a.getId().equals(targetId)) {
					a.setOrdine(i);
					break;
				}
			}
		}
		alimentoRepo.saveAll(alimenti);
	}

	// ═══════════════════════════════════════════
	// OWNERSHIP & HELPERS
	// ═══════════════════════════════════════════

	private SchedaTemplate getOwnedTemplate(Long templateId) {
		var me = currentUserService.getMe();
		SchedaTemplate st = templateRepo.findById(templateId)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		if (st.getCreatedBy() == null || !st.getCreatedBy().getId().equals(me.getId())) {
			throw new ForbiddenException("NON AUTORIZZATO: template scheda non accessibile");
		}
		return st;
	}

	private PastoSchedaTemplate getOwnedPasto(Long pastoId) {
		var me = currentUserService.getMe();
		return pastoRepo.findByIdAndSchedaTemplate_CreatedBy_Id(pastoId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: pasto template non accessibile"));
	}

	private AlimentoPastoSchedaTemplate getOwnedAlimento(Long aptId) {
		var me = currentUserService.getMe();
		return alimentoRepo.findByIdAndPastoSchedaTemplate_SchedaTemplate_CreatedBy_Id(aptId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alimento template non accessibile"));
	}

	private void checkPastoBelongsToTemplate(PastoSchedaTemplate pasto, Long templateId) {
		if (pasto.getSchedaTemplate() == null || !pasto.getSchedaTemplate().getId().equals(templateId)) {
			throw new BadRequestException("Pasto non appartenente al template indicato");
		}
	}

	private void checkAlimentoBelongsToTemplate(AlimentoPastoSchedaTemplate apt, Long templateId) {
		if (apt.getPastoSchedaTemplate() == null
				|| apt.getPastoSchedaTemplate().getSchedaTemplate() == null
				|| !apt.getPastoSchedaTemplate().getSchedaTemplate().getId().equals(templateId)) {
			throw new BadRequestException("Alimento non appartenente al template indicato");
		}
	}

	private String normalizeString(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeNomeCustom(String nomeCustom, String baseNome) {
		if (nomeCustom == null) return null;
		String trimmed = nomeCustom.trim();
		if (trimmed.isEmpty()) return null;
		if (baseNome != null && trimmed.equalsIgnoreCase(baseNome.trim())) return null;
		return trimmed;
	}

	private GiornoSettimana parseGiorno(String giorno) {
		if (giorno == null || giorno.isBlank()) return null;
		try {
			return GiornoSettimana.valueOf(giorno.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private LocalTime parseTime(String time) {
		if (time == null || time.isBlank()) return null;
		try {
			return LocalTime.parse(time);
		} catch (Exception e) {
			return null;
		}
	}
}
