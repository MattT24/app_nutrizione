package it.nutrizionista.restnutrizionista.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.RicettaDto;
import it.nutrizionista.restnutrizionista.dto.RicettaIngredienteDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateItemUpsertDto;
import it.nutrizionista.restnutrizionista.dto.PastoTemplateUpsertDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Ricetta;
import it.nutrizionista.restnutrizionista.entity.RicettaIngrediente;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.RicettaRepository;

@Service
public class RicettaService {

    @Autowired private RicettaRepository repo;
    @Autowired private PastoTemplateService pastoTemplateService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RicettaDto> listAll() {
        return repo.findAllPublicWithIngredienti()
                .stream()
                .map(DtoMapper::toRicettaDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RicettaDto getById(Long id) {
        Ricetta r = repo.findByIdWithIngredienti(id)
                .orElseThrow(() -> new NotFoundException("Ricetta non trovata: " + id));
        return DtoMapper.toRicettaDto(r);
    }

    // ── Import as Template ────────────────────────────────────────────────────

    /**
     * Crea un PastoTemplate a partire da una Ricetta.
     * Ogni RicettaIngrediente diventa un PastoTemplateItemUpsertDto 1:1.
     */
    @Transactional
    public PastoTemplateDto importAsTemplate(Long ricettaId) {
        Ricetta ricetta = repo.findByIdWithIngredienti(ricettaId)
                .orElseThrow(() -> new NotFoundException("Ricetta non trovata: " + ricettaId));

        PastoTemplateUpsertDto req = new PastoTemplateUpsertDto();
        req.setNome(ricetta.getTitolo());
        req.setDescrizione(ricetta.getDescrizione());

        List<PastoTemplateItemUpsertDto> items = new ArrayList<>();
        for (RicettaIngrediente ing : ricetta.getIngredienti()) {
            if (ing.getAlimento() == null) continue;
            PastoTemplateItemUpsertDto item = new PastoTemplateItemUpsertDto();
            item.setAlimentoId(ing.getAlimento().getId());
            item.setQuantita(ing.getQuantita());
            item.setNomeCustom(ing.getNomeCustom());
            item.setAlternative(new ArrayList<>());
            items.add(item);
        }
        req.setAlimenti(items);

        return pastoTemplateService.create(req);
    }

}

