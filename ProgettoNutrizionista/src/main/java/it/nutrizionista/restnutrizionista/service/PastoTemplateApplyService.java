package it.nutrizionista.restnutrizionista.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateMode;
import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateRequest;
import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateRestrizioniPolicy;
import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateResultDto;
import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateSkippedItemDto;
import it.nutrizionista.restnutrizionista.dto.PastoApplyTemplateStatsDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.PastoTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlimento;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlternativo;
import it.nutrizionista.restnutrizionista.entity.TipoRestrizione;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoDaEvitareRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoTemplateRepository;
import jakarta.validation.Valid;

@Service
public class PastoTemplateApplyService {
	@Autowired private OwnershipValidator ownershipValidator;
	@Autowired private CurrentUserService currentUserService;
	@Autowired private PastoRepository pastoRepository;
	@Autowired private PastoTemplateRepository templateRepository;
	@Autowired private AlimentoDaEvitareRepository daEvitareRepository;
	@Autowired private AlimentoAlternativoRepository alternativoRepository;

	@Transactional
	public PastoApplyTemplateResultDto applyToPasto(Long pastoId, @Valid PastoApplyTemplateRequest req) {
		ownershipValidator.getOwnedPasto(pastoId);

		Pasto pasto = pastoRepository.findByIdWithFullTree(pastoId)
				.orElseThrow(() -> new NotFoundException("Pasto non trovato"));

		PastoTemplate template = loadOwnedTemplate(req.getTemplateId());

		boolean pastoVuoto = pasto.getAlimentiPasto() == null || pasto.getAlimentiPasto().isEmpty();
		boolean pastoCustomEliminabile = Boolean.TRUE.equals(pasto.getEliminabile());

		PastoApplyTemplateMode mode = req.getMode() != null ? req.getMode() : PastoApplyTemplateMode.MERGE;
		if (pastoVuoto) mode = PastoApplyTemplateMode.REPLACE;

		PastoApplyTemplateRestrizioniPolicy restrizioniPolicy = req.getRestrizioniPolicy() != null
				? req.getRestrizioniPolicy()
				: PastoApplyTemplateRestrizioniPolicy.SKIP_WARNINGS;

		PastoApplyTemplateResultDto out = new PastoApplyTemplateResultDto();
		PastoApplyTemplateStatsDto stats = out.getStats();
		List<PastoApplyTemplateSkippedItemDto> skipped = out.getSkipped();

		if (pastoVuoto && pastoCustomEliminabile) {
			pasto.setNome(template.getNome());
			pasto.setDescrizione(template.getDescrizione());
		}

		Long clienteId = pastoRepository.findClienteIdByPastoId(pastoId)
				.orElseThrow(() -> new NotFoundException("Cliente non trovato per pasto"));

		Map<Long, PastoTemplateAlimento> templateByAlimentoId = (template.getAlimenti() == null ? List.<PastoTemplateAlimento>of() : template.getAlimenti())
				.stream()
				.filter(ti -> ti != null && ti.getAlimento() != null && ti.getAlimento().getId() != null)
				.collect(Collectors.toMap(ti -> ti.getAlimento().getId(), ti -> ti, (a, b) -> a));

		if (mode == PastoApplyTemplateMode.REPLACE) {
			int beforeAlt = countAlternative(pasto);
			int beforeAlimenti = pasto.getAlimentiPasto() != null ? pasto.getAlimentiPasto().size() : 0;

			pasto.getAlimentiPasto().removeIf(ap -> {
				Long alimentoId = ap.getAlimento() != null ? ap.getAlimento().getId() : null;
				return alimentoId == null || !templateByAlimentoId.containsKey(alimentoId);
			});

			int afterAlimenti = pasto.getAlimentiPasto() != null ? pasto.getAlimentiPasto().size() : 0;
			int afterAlt = countAlternative(pasto);

			stats.setRemovedAlimenti(stats.getRemovedAlimenti() + Math.max(0, beforeAlimenti - afterAlimenti));
			stats.setRemovedAlternative(stats.getRemovedAlternative() + Math.max(0, beforeAlt - afterAlt));
		}

		Map<Long, AlimentoPasto> apByAlimentoId = new HashMap<>();
		for (AlimentoPasto ap : pasto.getAlimentiPasto()) {
			if (ap.getAlimento() != null && ap.getAlimento().getId() != null) {
				apByAlimentoId.put(ap.getAlimento().getId(), ap);
			}
		}

		Set<Long> usedAltIds = new HashSet<>();
		for (AlimentoPasto ap : pasto.getAlimentiPasto()) {
			if (ap.getAlternative() == null) continue;
			for (AlimentoAlternativo alt : ap.getAlternative()) {
				Long altId = alt.getAlimentoAlternativo() != null ? alt.getAlimentoAlternativo().getId() : null;
				if (altId != null) usedAltIds.add(altId);
			}
		}

		for (PastoTemplateAlimento ti : templateByAlimentoId.values()) {
			Long alimentoId = ti.getAlimento().getId();
			RestrictionDecision restriction = checkRestriction(clienteId, alimentoId);
			if (restriction.blocked) {
				skipped.add(skipped("ALLERGIA", alimentoId, null, restriction.message));
				if (restrizioniPolicy == PastoApplyTemplateRestrizioniPolicy.FAIL_ON_WARNING) {
					throw new ForbiddenException(restriction.message);
				}
				continue;
			}
			if (restriction.warning) {
				skipped.add(skipped("WARNING_RESTRIZIONE", alimentoId, null, restriction.message));
				if (restrizioniPolicy == PastoApplyTemplateRestrizioniPolicy.FAIL_ON_WARNING) {
					throw new RuntimeException(restriction.message);
				}
				continue;
			}

			AlimentoPasto existing = apByAlimentoId.get(alimentoId);
			if (existing == null) {
				AlimentoPasto created = new AlimentoPasto();
				created.setPasto(pasto);
				created.setAlimento(ti.getAlimento());
				created.setQuantita(toIntGrammi(ti.getQuantita(), 100));
				applyNomeOverride(created, ti.getNomeCustom());

				pasto.getAlimentiPasto().add(created);
				apByAlimentoId.put(alimentoId, created);
				stats.setAddedAlimenti(stats.getAddedAlimenti() + 1);

				stats.setAddedAlternative(stats.getAddedAlternative() + applyTemplateAlternativesMerge(pasto, created, ti, usedAltIds, skipped));
				continue;
			}

			if (mode == PastoApplyTemplateMode.MERGE) {
				stats.setAddedAlternative(stats.getAddedAlternative() + applyTemplateAlternativesMerge(pasto, existing, ti, usedAltIds, skipped));
				continue;
			}

			boolean alimentoUpdated = false;
			int newQty = toIntGrammi(ti.getQuantita(), existing.getQuantita());
			if (existing.getQuantita() != newQty) {
				existing.setQuantita(newQty);
				alimentoUpdated = true;
			}
			boolean nomeUpdated = applyNomeOverride(existing, ti.getNomeCustom());
			if (nomeUpdated) alimentoUpdated = true;
			if (alimentoUpdated) stats.setUpdatedAlimenti(stats.getUpdatedAlimenti() + 1);

			AlternativeReplaceStats altStats = applyTemplateAlternativesReplace(pasto, existing, ti, usedAltIds, skipped);
			stats.setAddedAlternative(stats.getAddedAlternative() + altStats.added);
			stats.setUpdatedAlternative(stats.getUpdatedAlternative() + altStats.updated);
			stats.setRemovedAlternative(stats.getRemovedAlternative() + altStats.removed);
		}

		pastoRepository.save(pasto);

		Pasto refreshed = pastoRepository.findByIdWithFullTree(pastoId)
				.orElseThrow(() -> new NotFoundException("Pasto non trovato dopo apply"));
		out.setPasto(DtoMapper.toPastoDtoWithAssoc(refreshed));
		return out;
	}

	private PastoTemplate loadOwnedTemplate(Long templateId) {
		var me = currentUserService.getMe();
		PastoTemplate t = templateRepository.findByIdWithFullTree(templateId)
				.orElseThrow(() -> new NotFoundException("Template pasto non trovato"));
		if (t.getCreatedBy() == null || !t.getCreatedBy().getId().equals(me.getId())) {
			throw new ForbiddenException("NON AUTORIZZATO: template pasto non accessibile");
		}
		return t;
	}

	private int applyTemplateAlternativesMerge(Pasto pasto, AlimentoPasto ap, PastoTemplateAlimento ti, Set<Long> usedAltIds,
			List<PastoApplyTemplateSkippedItemDto> skipped) {
		if (ti.getAlternative() == null || ti.getAlternative().isEmpty()) return 0;

		Map<Long, AlimentoAlternativo> existingByAltId = new HashMap<>();
		if (ap.getAlternative() != null) {
			for (AlimentoAlternativo a : ap.getAlternative()) {
				if (a.getAlimentoAlternativo() != null && a.getAlimentoAlternativo().getId() != null) {
					existingByAltId.put(a.getAlimentoAlternativo().getId(), a);
				}
			}
		}

		int added = 0;
		for (PastoTemplateAlternativo ta : ti.getAlternative()) {
			Long altId = ta.getAlimentoAlternativo() != null ? ta.getAlimentoAlternativo().getId() : null;
			if (altId == null) continue;
			if (ap.getAlimento() != null && altId.equals(ap.getAlimento().getId())) {
				skipped.add(skipped("ALT_EQUALS_MAIN", ap.getAlimento().getId(), altId, "Alternativa uguale all'alimento principale"));
				continue;
			}
			if (existingByAltId.containsKey(altId)) continue;
			if (usedAltIds.contains(altId) || alternativoRepository.existsByPasto_IdAndAlimentoAlternativo_Id(pasto.getId(), altId)) {
				skipped.add(skipped("CONFLICT_PER_PASTO", ap.getAlimento().getId(), altId, "Alternativa già presente nel pasto"));
				continue;
			}
			AlimentoAlternativo created = toAlimentoAlternativo(ap, ta);
			ap.getAlternative().add(created);
			usedAltIds.add(altId);
			added++;
		}
		return added;
	}

	private AlternativeReplaceStats applyTemplateAlternativesReplace(Pasto pasto, AlimentoPasto ap, PastoTemplateAlimento ti,
			Set<Long> usedAltIds, List<PastoApplyTemplateSkippedItemDto> skipped) {
		AlternativeReplaceStats stats = new AlternativeReplaceStats();

		Map<Long, PastoTemplateAlternativo> templateAltById = new HashMap<>();
		if (ti.getAlternative() != null) {
			for (PastoTemplateAlternativo ta : ti.getAlternative()) {
				Long altId = ta.getAlimentoAlternativo() != null ? ta.getAlimentoAlternativo().getId() : null;
				if (altId != null) templateAltById.put(altId, ta);
			}
		}

		Map<Long, AlimentoAlternativo> existingByAltId = new HashMap<>();
		List<AlimentoAlternativo> toRemove = new ArrayList<>();
		for (AlimentoAlternativo a : ap.getAlternative()) {
			Long altId = a.getAlimentoAlternativo() != null ? a.getAlimentoAlternativo().getId() : null;
			if (altId == null) continue;
			existingByAltId.put(altId, a);
			if (!templateAltById.containsKey(altId)) {
				toRemove.add(a);
			}
		}
		for (AlimentoAlternativo r : toRemove) {
			Long altId = r.getAlimentoAlternativo() != null ? r.getAlimentoAlternativo().getId() : null;
			ap.getAlternative().remove(r);
			if (altId != null) usedAltIds.remove(altId);
			stats.removed++;
		}

		if (ti.getAlternative() != null) {
			for (PastoTemplateAlternativo ta : ti.getAlternative()) {
				Long altId = ta.getAlimentoAlternativo() != null ? ta.getAlimentoAlternativo().getId() : null;
				if (altId == null) continue;
				if (ap.getAlimento() != null && altId.equals(ap.getAlimento().getId())) {
					skipped.add(skipped("ALT_EQUALS_MAIN", ap.getAlimento().getId(), altId, "Alternativa uguale all'alimento principale"));
					continue;
				}

				AlimentoAlternativo existing = existingByAltId.get(altId);
				if (existing != null) {
					boolean changed = updateAlimentoAlternativoFromTemplate(existing, ta);
					if (changed) stats.updated++;
					continue;
				}

				if (usedAltIds.contains(altId) || alternativoRepository.existsByPasto_IdAndAlimentoAlternativo_Id(pasto.getId(), altId)) {
					skipped.add(skipped("CONFLICT_PER_PASTO", ap.getAlimento().getId(), altId, "Alternativa già presente nel pasto"));
					continue;
				}

				AlimentoAlternativo created = toAlimentoAlternativo(ap, ta);
				ap.getAlternative().add(created);
				usedAltIds.add(altId);
				stats.added++;
			}
		}

		return stats;
	}

	private boolean updateAlimentoAlternativoFromTemplate(AlimentoAlternativo target, PastoTemplateAlternativo src) {
		boolean changed = false;
		Integer q = src.getQuantita() != null ? src.getQuantita() : 100;
		if (!q.equals(target.getQuantita())) {
			target.setQuantita(q);
			changed = true;
		}
		Integer p = src.getPriorita() != null ? src.getPriorita() : 1;
		if (!p.equals(target.getPriorita())) {
			target.setPriorita(p);
			changed = true;
		}
		if (src.getMode() != null && src.getMode() != target.getMode()) {
			target.setMode(src.getMode());
			changed = true;
		}
		Boolean manual = src.getManual() != null ? src.getManual() : Boolean.TRUE;
		if (!manual.equals(target.getManual())) {
			target.setManual(manual);
			changed = true;
		}
		String note = normalizeDescrizione(src.getNote());
		if (note == null) {
			if (target.getNote() != null) {
				target.setNote(null);
				changed = true;
			}
		} else if (!note.equals(target.getNote())) {
			target.setNote(note);
			changed = true;
		}
		String nomeCustom = normalizeNomeCustom(src.getNomeCustom(), src.getAlimentoAlternativo() != null ? src.getAlimentoAlternativo().getNome() : null);
		if (nomeCustom == null) {
			if (target.getNomeCustom() != null) {
				target.setNomeCustom(null);
				changed = true;
			}
		} else if (!nomeCustom.equals(target.getNomeCustom())) {
			target.setNomeCustom(nomeCustom);
			changed = true;
		}
		return changed;
	}

	private AlimentoAlternativo toAlimentoAlternativo(AlimentoPasto ap, PastoTemplateAlternativo ta) {
		AlimentoAlternativo entity = new AlimentoAlternativo();
		entity.setAlimentoPasto(ap);
		entity.setPasto(ap.getPasto());
		entity.setAlimentoAlternativo(ta.getAlimentoAlternativo());
		entity.setQuantita(ta.getQuantita() != null ? ta.getQuantita() : 100);
		entity.setPriorita(ta.getPriorita() != null ? ta.getPriorita() : 1);
		entity.setMode(ta.getMode());
		entity.setManual(ta.getManual() != null ? ta.getManual() : Boolean.TRUE);
		entity.setNote(normalizeDescrizione(ta.getNote()));
		entity.setNomeCustom(normalizeNomeCustom(ta.getNomeCustom(), ta.getAlimentoAlternativo() != null ? ta.getAlimentoAlternativo().getNome() : null));
		return entity;
	}

	private boolean applyNomeOverride(AlimentoPasto ap, String nomeCustom) {
		String normalized = normalizeNomeCustom(nomeCustom, ap.getAlimento() != null ? ap.getAlimento().getNome() : null);
		if (normalized == null) {
			if (ap.getNomeOverride() != null) {
				ap.setNomeOverride(null);
				return true;
			}
			return false;
		}

		if (ap.getNomeOverride() == null) {
			AlimentoPastoNomeOverride ov = new AlimentoPastoNomeOverride();
			ov.setAlimentoPasto(ap);
			ov.setNomeCustom(normalized);
			ap.setNomeOverride(ov);
			return true;
		}

		if (!normalized.equals(ap.getNomeOverride().getNomeCustom())) {
			ap.getNomeOverride().setNomeCustom(normalized);
			return true;
		}
		return false;
	}

	private int countAlternative(Pasto pasto) {
		int total = 0;
		if (pasto.getAlimentiPasto() == null) return 0;
		for (AlimentoPasto ap : pasto.getAlimentiPasto()) {
			if (ap.getAlternative() != null) total += ap.getAlternative().size();
		}
		return total;
	}

	private int toIntGrammi(Double value, int fallback) {
		if (value == null) return fallback;
		int rounded = (int) Math.round(value);
		return Math.max(1, rounded);
	}

	private RestrictionDecision checkRestriction(Long clienteId, Long alimentoId) {
		RestrictionDecision out = new RestrictionDecision();
		AlimentoDaEvitare r = daEvitareRepository.findByCliente_IdAndAlimento_Id(clienteId, alimentoId).orElse(null);
		if (r == null) return out;
		if (r.getTipo() == TipoRestrizione.ALLERGIA) {
			out.blocked = true;
			out.message = "BLOCCO SICUREZZA: Il cliente è ALLERGICO a questo alimento. Inserimento vietato.";
			return out;
		}
		out.warning = true;
		out.message = "WARNING_RESTRIZIONE: Il cliente evita questo alimento per: " + r.getTipo();
		return out;
	}

	private PastoApplyTemplateSkippedItemDto skipped(String type, Long alimentoId, Long alternativaId, String message) {
		PastoApplyTemplateSkippedItemDto s = new PastoApplyTemplateSkippedItemDto();
		s.setType(type);
		s.setAlimentoId(alimentoId);
		s.setAlternativaId(alternativaId);
		s.setMessage(message);
		return s;
	}

	private String normalizeDescrizione(String descrizione) {
		if (descrizione == null) return null;
		String trimmed = descrizione.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeNomeCustom(String nomeCustom, String baseNome) {
		if (nomeCustom == null) return null;
		String trimmed = nomeCustom.trim();
		if (trimmed.isEmpty()) return null;
		if (baseNome != null && trimmed.equalsIgnoreCase(baseNome.trim())) return null;
		return trimmed;
	}

	private static class AlternativeReplaceStats {
		int added = 0;
		int updated = 0;
		int removed = 0;
	}

	private static class RestrictionDecision {
		boolean blocked = false;
		boolean warning = false;
		String message;
	}
}
