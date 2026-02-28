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
import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.TipoRestrizione;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoDaEvitareRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;

@Service
public class AlimentoPastoService {

    @Autowired private AlimentoPastoRepository repo;
    @Autowired private PastoRepository repoPasto;
    @Autowired private AlimentoBaseRepository repoAlimento;
    @Autowired private AlimentoDaEvitareRepository repoDaEvitare;
    @Autowired private AlimentoAlternativoService alimentoAlternativoService;

    @Transactional
    public PastoDto associaAlimento(AlimentoPastoRequest req) {
        Long pastoId = req.getPasto().getId();
        Long alimentoId = req.getAlimento().getId();
        
        // 1. Recupera il clienteId in modo leggero (1 sola query scalare, no Scheda/Cliente entity)
        Long clienteId = repoPasto.findClienteIdByPastoId(pastoId)
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
        
        AlimentoBase a = repoAlimento.findById(alimentoId)
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
        
        // 2. Controllo Restrizioni (Allergie)
        var restrizioneOpt = repoDaEvitare.findByCliente_IdAndAlimento_Id(clienteId, a.getId());

        if (restrizioneOpt.isPresent()) {
            AlimentoDaEvitare restrizione = restrizioneOpt.get();

            if (restrizione.getTipo() == TipoRestrizione.ALLERGIA) {
                throw new RuntimeException("BLOCCO SICUREZZA: Il cliente è ALLERGICO a '" + a.getNome() + "'. Inserimento vietato.");
            }

            if (!req.isForzaInserimento()) {
                throw new RuntimeException("WARNING_RESTRIZIONE: Il cliente evita '" + a.getNome() + "' per: " + restrizione.getTipo() + ". Vuoi forzare l'inserimento?");
            }
        }

        // 3. Controllo Duplicati
        if (repo.existsByPasto_IdAndAlimento_Id(pastoId, a.getId())) {
             throw new RuntimeException("Alimento già presente nel pasto");
        }

        // 4. Salvataggio — serve un Pasto reference (basta un proxy, non serve il full tree)
        Pasto pastoRef = repoPasto.getReferenceById(pastoId);
        AlimentoPasto associazione = new AlimentoPasto(a, pastoRef, req.getQuantita());
        repo.save(associazione);
        
        // 5. Carica l'albero completo solo per il response mapping (1 sola query)
        Pasto refreshed = repoPasto.findByIdWithFullTree(pastoId)
                .orElseThrow(() -> new RuntimeException("Pasto non trovato dopo salvataggio"));
        
        return DtoMapper.toPastoDtoWithAssoc(refreshed);
    }
    
    @Transactional
    public PastoDto eliminaAssociazione(Long pastoId, Long alimentoId) {
        // Carica il pasto con albero completo
        Pasto p = repoPasto.findByIdWithFullTree(pastoId)
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
        
        // Rimuovi l'alimento dalla collection - orphanRemoval=true lo cancellerà dal DB
        if (p.getAlimentiPasto() != null) {
            p.getAlimentiPasto().removeIf(ap -> 
                ap.getAlimento() != null && ap.getAlimento().getId().equals(alimentoId)
            );
        }
        
        // Salva per forzare il flush al DB
        repoPasto.save(p);
        
        return DtoMapper.toPastoDtoWithAssoc(p);
    }
    
    // Metodo legacy per compatibilità (opzionale)
    @Transactional
    public PastoDto eliminaAssociazione(AlimentoPastoRequest req) {
        return eliminaAssociazione(req.getPasto().getId(), req.getAlimento().getId());
    }
    
    @Transactional(readOnly = true)
    public List<AlimentoBaseDto> listAlimentiByPasto(Long pastoId) {
        return repo.findByPasto_Id(pastoId).stream()
                .map(AlimentoPasto::getAlimento)
                .map(DtoMapper::toAlimentoBaseDtoLight)
                .collect(Collectors.toList());
    }

    @Transactional
    public PastoDto aggiornaQuantita(AlimentoPastoRequest req){
        // OTTIMIZZAZIONE: Cerchiamo direttamente l'associazione.
        // Non serve caricare prima Pasto e Alimento separatamente.
        AlimentoPasto ap = repo.findByPasto_IdAndAlimento_Id(req.getPasto().getId(), req.getAlimento().getId())
                .orElseThrow(() -> new RuntimeException("Associazione alimento-pasto non trovata"));
        
        // Aggiorna quantità
        ap.setQuantita(req.getQuantita());
        repo.save(ap);

        alimentoAlternativoService.recomputeAutoAlternativesForAlimentoPasto(ap);
        
        // Ri-carica con albero completo per il mapper
        Pasto refreshed = repoPasto.findByIdWithFullTree(ap.getPasto().getId())
                .orElseThrow(() -> new RuntimeException("Pasto non trovato dopo aggiornamento"));
        
        return DtoMapper.toPastoDtoWithAssoc(refreshed);
    }
}

