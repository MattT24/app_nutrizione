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

    @Transactional
    public PastoDto associaAlimento(AlimentoPastoRequest req) {
        // 1. Recupera Entità
        Pasto p = repoPasto.findById(req.getPasto().getId())
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
        
        AlimentoBase a = repoAlimento.findById(req.getAlimento().getId())
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
        
        // 2. Controllo Restrizioni (Allergie)
        Long clienteId = p.getScheda().getCliente().getId();
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
        if (repo.existsByPasto_IdAndAlimento_Id(p.getId(), a.getId())) {
             throw new RuntimeException("Alimento già presente nel pasto");
        }

        // 4. Salvataggio
        AlimentoPasto associazione = new AlimentoPasto(a, p, req.getQuantita());
        repo.save(associazione);
        
        // 5. IMPORTANTE: Aggiorna la lista in memoria per il return!
        // Hibernate potrebbe non aggiornare automaticamente la collezione inversa nella stessa transazione
        p.getAlimentiPasto().add(associazione); 
        
        return DtoMapper.toPastoDtoWithAssoc(p);
    }
    
    @Transactional
    public PastoDto eliminaAssociazione(Long pastoId, Long alimentoId) {
        repo.deleteByPasto_IdAndAlimento_Id(pastoId, alimentoId);
        
        // Ricarichiamo il pasto per restituirlo aggiornato (senza l'alimento cancellato)
        Pasto p = repoPasto.findById(pastoId)
                .orElseThrow(() -> new RuntimeException("Pasto non trovato"));
                
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
        
        // Restituisci il pasto padre (che è già collegato all'associazione)
        return DtoMapper.toPastoDtoWithAssoc(ap.getPasto());
    }
}