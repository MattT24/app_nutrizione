package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoRequest;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;


@Service
public class AlimentoPastoService {

	@Autowired private AlimentoPastoRepository repo;
	@Autowired private PastoRepository repoPasto;
	@Autowired private AlimentoBaseRepository repoAlimento;

	@Transactional
    public PastoDto associaAlimento(AlimentoPastoRequest req) {
        Pasto p = repoPasto.findById(req.getPasto().getId())
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
        
        AlimentoBase a = repoAlimento.findById(req.getAlimento().getId())
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));

        if (repo.existsByPasto_IdAndAlimento_Id(p.getId(), a.getId())) {
             throw new RuntimeException("Alimento già presente nel pasto");
        }

        AlimentoPasto associazione = new AlimentoPasto(a, p, req.getQuantita());
        repo.save(associazione);
        p.getAlimentiPasto().add(associazione); 

        return DtoMapper.toPastoDtoWithAssoc(p);
    }
	
    @Transactional
    public PastoDto eliminaAssociazione(AlimentoPastoRequest req) {
        repo.deleteByPasto_IdAndAlimento_Id(req.getPasto().getId(), req.getAlimento().getId());
        Pasto p = repoPasto.findById(req.getPasto().getId())
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
        return DtoMapper.toPastoDtoWithAssoc(p);
    }
    
    @Transactional(readOnly = true)
    public List<AlimentoBaseDto> listAlimentyByPasto(Long pastoId) {
        return repo.findByPasto_Id(pastoId).stream()
                .map(AlimentoPasto::getAlimento)
                .map(DtoMapper::toAlimentoBaseDtoLight)
                .collect(Collectors.toList());
    }

    @Transactional
    public PastoDto aggiornaQuantita(AlimentoPastoRequest req){
        Pasto p = repoPasto.findById(req.getPasto().getId())
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
        AlimentoBase a = repoAlimento.findById(req.getAlimento().getId())
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
        AlimentoPasto ap = repo.findByPasto_IdAndAlimento_Id(p.getId(), a.getId())
                .orElseThrow(() -> new RuntimeException("Associazione non trovata"));
        ap.setQuantita(req.getQuantita());
        repo.save(ap);
        return DtoMapper.toPastoDtoWithAssoc(p);
    }
}
