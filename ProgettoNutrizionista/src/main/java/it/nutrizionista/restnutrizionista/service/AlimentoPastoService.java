package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoPastoRequest;
import it.nutrizionista.restnutrizionista.dto.MotivoValutazioneDto;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;

@Service
public class AlimentoPastoService {

    @Autowired private AlimentoPastoRepository repo;
    @Autowired private PastoRepository repoPasto;
    @Autowired private AlimentoBaseRepository repoAlimento;
    @Autowired private ClinicalEngineService clinicalEngineService;
    @Autowired private AlimentoAlternativoService alimentoAlternativoService;
    @Autowired private OwnershipValidator ownershipValidator;
    @Autowired private LimitazioneTrattamentoValidator limitazioneValidator;
    @Autowired private AuditService auditService;
    @Autowired private SchedaFascicoloSync fascicoloSync;

    @Transactional
    public PastoDto associaAlimento(AlimentoPastoRequest req) {
        Long pastoId = req.getPasto().getId();
        Long alimentoId = req.getAlimento().getId();

        // 1. Ownership: il pasto deve appartenere al nutrizionista loggato (403 altrimenti).
        //    Difesa in profondità: il @PreAuthorize sul controller verifica solo il PERMESSO, non la
        //    proprietà dell'istanza. Da qui ricaviamo anche cliente/schedaId (serve all'audit fidato).
        Pasto pasto = ownershipValidator.getOwnedPasto(pastoId);
        Cliente cliente = pasto.getScheda().getCliente();
        limitazioneValidator.assertNonLimitato(cliente); // A5.3
        Long clienteId = cliente.getId();
        Long schedaId = pasto.getScheda().getId();

        // AlimentoBase è catalogo misto (globale + personale), NON risorsa-cliente → findById non-scoped.
        AlimentoBase a = repoAlimento.findById(alimentoId)
                .orElseThrow(() -> new NotFoundException("Alimento non trovato"));

        // 2. Controllo Restrizioni cliniche via MDSS
        ValutazioneClinicaDto valutazione = clinicalEngineService.valuta(a, cliente);
        String msg = valutazione.motivi().isEmpty()
                ? "Motivo clinico non specificato"
                : valutazione.motivi().stream().map(MotivoValutazioneDto::messaggio).collect(Collectors.joining(" "));

        // Blocco GRAVE (allergene dichiarato, sale oltre soglia grave, avversione grave): superabile
        // SOLO con la conferma consapevole dedicata (posizionamento anti-MDR: decide il professionista).
        if (valutazione.stato() == LivelloAllerta.ALERT_GRAVE && !req.isConfermaBloccoGrave()) {
            throw new BadRequestException("BLOCCO SICUREZZA: " + msg
                    + " Confermi consapevolmente l'inserimento sotto la tua responsabilità professionale?");
        }

        // Blocco WARNING (restrizione/intolleranza): superabile con forzaInserimento.
        if (valutazione.stato() == LivelloAllerta.WARNING && !req.isForzaInserimento()) {
            throw new BadRequestException("WARNING_RESTRIZIONE: " + msg + " Vuoi forzare l'inserimento?");
        }

        // 3. Controllo Duplicati
        if (repo.existsByPasto_IdAndAlimento_Id(pastoId, a.getId())) {
             throw new BadRequestException("Alimento già presente nel pasto");
        }

        // 4. Audit dell'override consapevole (solo se un blocco GRAVE è stato forzato): stessa tx del save
        //    → la riga di audit committa iff l'inserimento committa. Registrato PRIMA del save.
        if (valutazione.stato() == LivelloAllerta.ALERT_GRAVE) {
            String dettaglio = "Override BLOCCO SICUREZZA — alimento '" + a.getNome() + "' (id=" + a.getId()
                    + ") pasto id=" + pastoId + ". Motivi: " + msg;
            if (dettaglio.length() > 1024) dettaglio = dettaglio.substring(0, 1024);
            auditService.recordOverrideSameTx(AuditEntityType.SCHEDA, schedaId, clienteId, dettaglio);
        }

        // 5. Salvataggio — riusa il Pasto già gestito (owned) dell'ownership check.
        AlimentoPasto associazione = new AlimentoPasto(a, pasto, req.getQuantita());
        repo.save(associazione);

        // 6. Carica l'albero completo solo per il response mapping (1 sola query)
        Pasto refreshed = repoPasto.findByIdWithFullTree(pastoId)
                .orElseThrow(() -> new NotFoundException("Pasto non trovato dopo salvataggio"));

        fascicoloSync.sincronizza(clienteId, schedaId);
        return DtoMapper.toPastoDtoWithAssoc(refreshed);
    }
    
    @Transactional
    public PastoDto eliminaAssociazione(Long pastoId, Long alimentoId) {
        // F-OWN-SWEEP (ex F-D1b): valida la proprietà del pasto prima di mutarlo (403 se cross-tenant).
        Pasto owned = ownershipValidator.getOwnedPasto(pastoId);
        limitazioneValidator.assertNonLimitato(owned.getScheda().getCliente()); // A5.3
        // Carica il pasto con albero completo
        Pasto p = repoPasto.findByIdWithFullTree(pastoId)
                .orElseThrow(() -> new NotFoundException("Pasto non trovato"));
        
        // Rimuovi l'alimento dalla collection - orphanRemoval=true lo cancellerà dal DB
        if (p.getAlimentiPasto() != null) {
            p.getAlimentiPasto().removeIf(ap -> 
                ap.getAlimento() != null && ap.getAlimento().getId().equals(alimentoId)
            );
        }
        
        // Salva per forzare il flush al DB
        repoPasto.save(p);

        fascicoloSync.sincronizza(owned.getScheda().getCliente().getId(), owned.getScheda().getId());
        return DtoMapper.toPastoDtoWithAssoc(p);
    }
    
    // Metodo legacy per compatibilità (opzionale)
    @Transactional
    public PastoDto eliminaAssociazione(AlimentoPastoRequest req) {
        return eliminaAssociazione(req.getPasto().getId(), req.getAlimento().getId());
    }
    
    @Transactional(readOnly = true)
    public List<AlimentoBaseDto> listAlimentiByPasto(Long pastoId) {
        // F-OWN-SWEEP: nega la lettura degli alimenti del pasto di un altro tenant (403).
        ownershipValidator.getOwnedPasto(pastoId);
        return repo.findByPasto_Id(pastoId).stream()
                .map(AlimentoPasto::getAlimento)
                .map(DtoMapper::toAlimentoBaseDtoLight)
                .collect(Collectors.toList());
    }

    @Transactional
    public PastoDto aggiornaQuantita(AlimentoPastoRequest req){
        // F-OWN-SWEEP (ex F-D1b): valida la proprietà del pasto prima di modificarne un alimento (403 se cross-tenant).
        Pasto ownedPasto = ownershipValidator.getOwnedPasto(req.getPasto().getId());
        limitazioneValidator.assertNonLimitato(ownedPasto.getScheda().getCliente()); // A5.3
        // OTTIMIZZAZIONE: Cerchiamo direttamente l'associazione.
        // Non serve caricare prima Pasto e Alimento separatamente.
        AlimentoPasto ap = repo.findByPasto_IdAndAlimento_Id(req.getPasto().getId(), req.getAlimento().getId())
                .orElseThrow(() -> new NotFoundException("Associazione alimento-pasto non trovata"));
        
        // Aggiorna quantità
        ap.setQuantita(req.getQuantita());
        repo.save(ap);

        alimentoAlternativoService.recomputeAutoAlternativesForAlimentoPasto(ap);

        // Ri-carica con albero completo per il mapper
        Pasto refreshed = repoPasto.findByIdWithFullTree(ap.getPasto().getId())
                .orElseThrow(() -> new NotFoundException("Pasto non trovato dopo aggiornamento"));

        fascicoloSync.sincronizza(ownedPasto.getScheda().getCliente().getId(), ownedPasto.getScheda().getId());
        return DtoMapper.toPastoDtoWithAssoc(refreshed);
    }
}

