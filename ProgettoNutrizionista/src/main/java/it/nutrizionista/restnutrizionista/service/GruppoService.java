package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.GruppoDto;
import it.nutrizionista.restnutrizionista.dto.GruppoFormDto;
import it.nutrizionista.restnutrizionista.dto.GruppoPermessiRequest;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.Gruppo;
import it.nutrizionista.restnutrizionista.entity.Permesso;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.GruppoRepository;
import it.nutrizionista.restnutrizionista.repository.PermessoRepository;

import java.util.List;
import java.util.stream.Collectors;

/** Logica di business per Gruppi. */
@Service
public class GruppoService {

    @Autowired private GruppoRepository gruppoRepo;
    @Autowired private PermessoRepository permessoRepo;

    /** Crea un gruppo (nessun permesso associato di default). */
    @Transactional
    public GruppoDto create(GruppoFormDto form) {
        Gruppo g = new Gruppo();
        g.setNome(form.getNome());
        g.setAlias(form.getAlias());
        return DtoMapper.toGruppoDtoLight(gruppoRepo.save(g));
    }

    /** Aggiorna nome/alias gruppo. */
    @Transactional
    public GruppoDto update(GruppoFormDto form) {
        if (form.getId() == null) throw new RuntimeException("Id gruppo obbligatorio per update");
        Gruppo g = gruppoRepo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Gruppo non trovato"));
        g.setNome(form.getNome());
        g.setAlias(form.getAlias());
        return DtoMapper.toGruppoDtoLight(gruppoRepo.save(g));
    }

    /** Elimina gruppo per id. */
    @Transactional
    public void delete(Long id) { gruppoRepo.deleteById(id); }

    /** Dettaglio gruppo: con o senza permessi. */
    @Transactional(readOnly = true)
    public GruppoDto getById(Long id, boolean withPermessi) {
        Gruppo g = gruppoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Gruppo non trovato"));
        return withPermessi ? DtoMapper.toGruppoDtoWithPermessi(g)
                            : DtoMapper.toGruppoDtoLight(g);
    }

    /** Lista paginata gruppi. */
    @Transactional(readOnly = true)
    public PageResponse<GruppoDto> list(Pageable pageable, boolean withPermessi) {
        Page<Gruppo> page = gruppoRepo.findAll(pageable);
        return PageResponse.from(
                page.map(g -> withPermessi ? DtoMapper.toGruppoDtoWithPermessi(g)
                                           : DtoMapper.toGruppoDtoLight(g))
        );
    }

    /** Lista completa non paginata. */
    @Transactional(readOnly = true)
    public List<GruppoDto> listAll(boolean withPermessi) {
        return gruppoRepo.findAll().stream()
                .map(g -> withPermessi ? DtoMapper.toGruppoDtoWithPermessi(g)
                                       : DtoMapper.toGruppoDtoLight(g))
                .collect(Collectors.toList());
    }

    /** Associa i permessi al gruppo impostando il gruppo nei permessi. */
    public GruppoDto associaPermessi(GruppoPermessiRequest req) {
        Gruppo g = gruppoRepo.findById(req.getGruppoId())
                .orElseThrow(() -> new RuntimeException("Gruppo non trovato"));
        for (var pDto : req.getPermessi()) {
            Permesso p = permessoRepo.findById(pDto.getId())
                    .orElseThrow(() -> new RuntimeException("Permesso non trovato id=" + pDto.getId()));
            p.setGruppo(g);
            permessoRepo.save(p);
        }
        return DtoMapper.toGruppoDtoWithPermessi(g); // includo permessi aggiornati
    }

    /** Dissocia i permessi dal gruppo (gruppo = null). */
    public GruppoDto dissociaPermessi(GruppoPermessiRequest req) {
        gruppoRepo.findById(req.getGruppoId())
                .orElseThrow(() -> new RuntimeException("Gruppo non trovato"));
        for (var pDto : req.getPermessi()) {
            Permesso p = permessoRepo.findById(pDto.getId())
                    .orElseThrow(() -> new RuntimeException("Permesso non trovato id=" + pDto.getId()));
            if (p.getGruppo() != null && p.getGruppo().getId().equals(req.getGruppoId())) {
                p.setGruppo(null);
                permessoRepo.save(p);
            }
        }
        Gruppo g = gruppoRepo.findById(req.getGruppoId()).orElseThrow();
        return DtoMapper.toGruppoDtoWithPermessi(g); // includo permessi aggiornati
    }
}
