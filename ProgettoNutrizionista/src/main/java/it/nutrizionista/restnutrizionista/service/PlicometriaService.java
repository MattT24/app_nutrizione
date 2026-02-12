package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PlicometriaDto;
import it.nutrizionista.restnutrizionista.dto.PlicometriaFormDto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import jakarta.validation.Valid;

@Service
public class PlicometriaService {
	
	@Autowired private PlicometriaCalcoliService calcoliService;

    @Autowired private PlicometriaRepository repo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private UtenteRepository utenteRepo;

    // Helper per ottenere l'utente loggato (Nutrizionista)
    private Utente getMe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
    }

    @Transactional
    public PlicometriaDto create(@Valid PlicometriaFormDto form) {
        Utente me = getMe();
        
        // Verifica che il cliente esista E appartenga al nutrizionista loggato
        Cliente cliente = clienteRepo.findByIdAndNutrizionista_Id(form.getCliente().getId(), me.getId())
                .orElseThrow(() -> new RuntimeException("Cliente non trovato o non autorizzato"));

        // Conversione DTO -> Entity
        Plicometria p = DtoMapper.toPlicometria(form);
        p.setCliente(cliente); // Associo la plicometria al cliente
        var res = calcoliService.calcola(p, cliente);

        p.setPesoKgRiferimento(cliente.getPeso());
        p.setSommaPliche(res.sommaPliche());
        p.setDensitaCorporea(res.densitaCorporea());
        p.setPercentualeMassaGrassa(res.percentualeMassaGrassa());
        p.setMassaGrassaKg(res.massaGrassaKg());
        p.setMassaMagraKg(res.massaMagraKg());

        // Nota: Il calcolo della % massa grassa potrebbe essere fatto qui se non arriva dal FE
        // es: p.setPercentualeMassaGrassa(CalcoliService.calcola(p));

        return DtoMapper.toPlicometriaDto(repo.save(p));
    }

    @Transactional
    public PlicometriaDto update(@Valid PlicometriaFormDto form) {
        Utente me = getMe();
        
        if (form.getId() == null) {
            throw new RuntimeException("Id Plicometria obbligatorio per update");
        }

        Plicometria p = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Plicometria non trovata"));

        // Controllo di sicurezza: la plicometria appartiene a un cliente del nutrizionista loggato?
        if (!p.getCliente().getNutrizionista().getId().equals(me.getId())) {
            throw new RuntimeException("Non autorizzato a modificare questa plicometria");
        }

        // Aggiorno i campi usando il Mapper
        DtoMapper.updatePlicometriaFromForm(p, form);
        
        var res = calcoliService.calcola(p, p.getCliente());

        p.setPesoKgRiferimento(p.getCliente().getPeso());
        p.setSommaPliche(res.sommaPliche());
        p.setDensitaCorporea(res.densitaCorporea());
        p.setPercentualeMassaGrassa(res.percentualeMassaGrassa());
        p.setMassaGrassaKg(res.massaGrassaKg());
        p.setMassaMagraKg(res.massaMagraKg());

        
        return DtoMapper.toPlicometriaDto(repo.save(p));
    }

    @Transactional
    public void delete(Long id) { 
        Utente me = getMe();
        
        Plicometria p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plicometria non trovata"));

        // Controllo di sicurezza
        if (!p.getCliente().getNutrizionista().getId().equals(me.getId())) {
            throw new RuntimeException("Non autorizzato a eliminare questa plicometria");
        }

        repo.deleteById(id); 
    }

    @Transactional(readOnly = true)
    public PageResponse<PlicometriaDto> allPlicometrieByCliente(Long clienteId, Pageable pageable) {
        Utente me = getMe();
        
        // Verifica preliminare che il cliente sia del nutrizionista
        boolean isMioCliente = clienteRepo.findByIdAndNutrizionista_Id(clienteId, me.getId()).isPresent();
        if (!isMioCliente) {
            throw new RuntimeException("Cliente non trovato o non autorizzato");
        }

        // Recupero paginato ordinato per data decrescente
        return PageResponse.from(
            repo.findByCliente_IdOrderByDataMisurazioneDesc(clienteId, pageable)
                .map(DtoMapper::toPlicometriaDto)
        );
    }
}