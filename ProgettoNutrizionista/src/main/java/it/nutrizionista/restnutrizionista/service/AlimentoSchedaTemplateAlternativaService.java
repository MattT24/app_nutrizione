package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoSchedaTemplateAlternativaDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoSchedaTemplateAlternativaFormDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.AlimentoSchedaTemplateAlternativa;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoSchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoSchedaTemplateAlternativaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaTemplateRepository;
import jakarta.validation.Valid;

/**
 * Service per la gestione degli alimenti alternativi nei template.
 * Mirror di AlimentoAlternativoService adattato al dominio SchedaTemplate.
 */
@Service
public class AlimentoSchedaTemplateAlternativaService {

    @Autowired
    private AlimentoSchedaTemplateAlternativaRepository repo;

    @Autowired
    private AlimentoPastoSchedaTemplateRepository alimentoPastoRepo;

    @Autowired
    private AlimentoBaseRepository alimentoBaseRepo;

    @Autowired
    private SchedaTemplateRepository templateRepo;

    @Autowired
    private CurrentUserService currentUserService;

    // ═══════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════

    /**
     * Crea una nuova alternativa per un alimento nel pasto template
     */
    @Transactional
    public AlimentoSchedaTemplateAlternativaDto create(Long templateId, Long aptId,
            @Valid AlimentoSchedaTemplateAlternativaFormDto form) {
        getOwnedTemplate(templateId);
        AlimentoPastoSchedaTemplate apt = getOwnedAlimentoPasto(aptId);
        checkAptBelongsToTemplate(apt, templateId);

        // Verifica che l'alimento alternativo esista
        AlimentoBase alimentoAlt = alimentoBaseRepo.findById(form.alimentoAlternativoId())
                .orElseThrow(() -> new NotFoundException(
                        "Alimento alternativo non trovato con id: " + form.alimentoAlternativoId()));

        // Verifica che l'alimento alternativo sia diverso dall'alimento principale
        if (apt.getAlimento() != null &&
                apt.getAlimento().getId().equals(form.alimentoAlternativoId())) {
            throw new BadRequestException("L'alimento alternativo deve essere diverso dall'alimento principale");
        }

        // Verifica duplicati
        if (repo.existsByAlimentoPastoSchedaTemplate_IdAndAlimentoAlternativo_Id(
                aptId, form.alimentoAlternativoId())) {
            throw new ConflictException("Questa alternativa esiste già per questo alimento nel pasto");
        }

        // Calcola la prossima priorità se non specificata
        Integer priorita = form.priorita();
        if (priorita == null || priorita < 1) {
            priorita = (int) repo.countByAlimentoPastoSchedaTemplate_Id(aptId) + 1;
        }

        AlternativeMode mode = form.mode() != null ? parseMode(form.mode()) : AlternativeMode.CALORIE;
        boolean manual = Boolean.TRUE.equals(form.manual());

        // Calcolo quantità basato su calorie se non manual
        Integer quantita = form.quantita();
        if (!manual && quantita == null) {
            quantita = suggestQuantityByCalorie(apt, alimentoAlt, mode);
        }

        AlimentoSchedaTemplateAlternativa entity = new AlimentoSchedaTemplateAlternativa();
        entity.setAlimentoPastoSchedaTemplate(apt);
        entity.setAlimentoAlternativo(alimentoAlt);
        entity.setQuantita(quantita != null ? quantita : 100);
        entity.setPriorita(priorita);
        entity.setMode(mode);
        entity.setManual(manual);
        entity.setNote(form.note());

        return DtoMapper.toAlimentoSchedaTemplateAlternativaDto(repo.save(entity));
    }

    /**
     * Aggiorna un'alternativa esistente (quantità, priorità, mode, manual, note)
     */
    @Transactional
    public AlimentoSchedaTemplateAlternativaDto update(Long templateId, Long aptId, Long altId,
            @Valid AlimentoSchedaTemplateAlternativaFormDto form) {
        getOwnedTemplate(templateId);
        AlimentoSchedaTemplateAlternativa entity = getOwnedAlternativa(altId);
        checkAlternativaBelongsToApt(entity, aptId);
        checkAptBelongsToTemplate(entity.getAlimentoPastoSchedaTemplate(), templateId);

        // Aggiorna solo i campi modificabili
        if (form.quantita() != null) {
            entity.setQuantita(form.quantita());
        }
        if (form.priorita() != null) {
            entity.setPriorita(form.priorita());
        }
        if (form.mode() != null) {
            entity.setMode(parseMode(form.mode()));
        }
        if (form.manual() != null) {
            entity.setManual(form.manual());
        }
        if (form.note() != null) {
            entity.setNote(form.note());
        }

        // Ricalcola se non manual e qty non specificata
        if (Boolean.FALSE.equals(entity.getManual()) && form.quantita() == null) {
            AlimentoPastoSchedaTemplate apt = entity.getAlimentoPastoSchedaTemplate();
            Integer suggested = suggestQuantityByCalorie(apt, entity.getAlimentoAlternativo(), entity.getMode());
            if (suggested != null) {
                entity.setQuantita(suggested);
            }
        }

        return DtoMapper.toAlimentoSchedaTemplateAlternativaDto(repo.save(entity));
    }

    /**
     * Elimina un'alternativa per ID
     */
    @Transactional
    public void delete(Long templateId, Long aptId, Long altId) {
        getOwnedTemplate(templateId);
        AlimentoSchedaTemplateAlternativa entity = getOwnedAlternativa(altId);
        checkAlternativaBelongsToApt(entity, aptId);
        checkAptBelongsToTemplate(entity.getAlimentoPastoSchedaTemplate(), templateId);
        repo.delete(entity);
    }

    /**
     * Lista tutte le alternative per un alimento in pasto template, ordinate per priorità
     */
    @Transactional(readOnly = true)
    public List<AlimentoSchedaTemplateAlternativaDto> listByAlimentoPasto(Long templateId, Long aptId) {
        getOwnedTemplate(templateId);
        getOwnedAlimentoPasto(aptId);
        return repo.findByAlimentoPastoSchedaTemplate_IdOrderByPrioritaAsc(aptId).stream()
                .map(DtoMapper::toAlimentoSchedaTemplateAlternativaDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════
    // DISPLAY NAME
    // ═══════════════════════════════════════════

    @Transactional
    public AlimentoSchedaTemplateAlternativaDto setDisplayName(Long templateId, Long aptId,
            Long altId, String nome) {
        getOwnedTemplate(templateId);
        AlimentoSchedaTemplateAlternativa alt = getOwnedAlternativa(altId);
        checkAlternativaBelongsToApt(alt, aptId);
        checkAptBelongsToTemplate(alt.getAlimentoPastoSchedaTemplate(), templateId);
        alt.setNomeCustom(nome);
        return DtoMapper.toAlimentoSchedaTemplateAlternativaDto(repo.save(alt));
    }

    @Transactional
    public AlimentoSchedaTemplateAlternativaDto deleteDisplayName(Long templateId, Long aptId, Long altId) {
        getOwnedTemplate(templateId);
        AlimentoSchedaTemplateAlternativa alt = getOwnedAlternativa(altId);
        checkAlternativaBelongsToApt(alt, aptId);
        checkAptBelongsToTemplate(alt.getAlimentoPastoSchedaTemplate(), templateId);
        alt.setNomeCustom(null);
        return DtoMapper.toAlimentoSchedaTemplateAlternativaDto(repo.save(alt));
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

    private AlimentoPastoSchedaTemplate getOwnedAlimentoPasto(Long aptId) {
        var me = currentUserService.getMe();
        return alimentoPastoRepo.findByIdAndPastoSchedaTemplate_SchedaTemplate_CreatedBy_Id(aptId, me.getId())
                .orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alimento template non accessibile"));
    }

    private AlimentoSchedaTemplateAlternativa getOwnedAlternativa(Long altId) {
        var me = currentUserService.getMe();
        return repo.findByIdAndAlimentoPastoSchedaTemplate_PastoSchedaTemplate_SchedaTemplate_CreatedBy_Id(
                        altId, me.getId())
                .orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alternativa template non accessibile"));
    }

    private void checkAptBelongsToTemplate(AlimentoPastoSchedaTemplate apt, Long templateId) {
        if (apt.getPastoSchedaTemplate() == null
                || apt.getPastoSchedaTemplate().getSchedaTemplate() == null
                || !apt.getPastoSchedaTemplate().getSchedaTemplate().getId().equals(templateId)) {
            throw new BadRequestException("Alimento non appartenente al template indicato");
        }
    }

    private void checkAlternativaBelongsToApt(AlimentoSchedaTemplateAlternativa alt, Long aptId) {
        if (alt.getAlimentoPastoSchedaTemplate() == null
                || !alt.getAlimentoPastoSchedaTemplate().getId().equals(aptId)) {
            throw new BadRequestException("Alternativa non associata all'alimento nel pasto indicato");
        }
    }

    private AlternativeMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) return AlternativeMode.CALORIE;
        try {
            return AlternativeMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AlternativeMode.CALORIE;
        }
    }

    /**
     * Calcolo inline della quantità suggerita basandosi sulle calorie per 100g.
     * Equivalente a AlternativeSuggestionCalculator.suggestQuantity,
     * adattato per AlimentoPastoSchedaTemplate (che non ha un AlimentoPasto).
     */
    private Integer suggestQuantityByCalorie(AlimentoPastoSchedaTemplate apt,
            AlimentoBase alimentoAlt, AlternativeMode mode) {
        if (apt == null || apt.getAlimento() == null || alimentoAlt == null) return 100;
        AlimentoBase orig = apt.getAlimento();

        // Calcola kcal per 100g di entrambi gli alimenti
        double origKcal = macroKcalPer100g(orig);
        double altKcal = macroKcalPer100g(alimentoAlt);

        if (altKcal <= 0) return 100;

        // kcal dell'alimento originale nella quantità del pasto
        double targetKcal = (origKcal / 100.0) * apt.getQuantita();
        // quantità alternativa per ottenere le stesse kcal
        int suggested = (int) Math.round((targetKcal / altKcal) * 100.0);
        return Math.max(1, suggested);
    }

    private double macroKcalPer100g(AlimentoBase alimento) {
        if (alimento == null || alimento.getMacroNutrienti() == null) return 0;
        var macro = alimento.getMacroNutrienti();
        double carb = macro.getCarboidrati() != null ? macro.getCarboidrati() : 0;
        double prot = macro.getProteine() != null ? macro.getProteine() : 0;
        double fat = macro.getGrassi() != null ? macro.getGrassi() : 0;
        return (carb * 4) + (prot * 4) + (fat * 9);
    }
}
