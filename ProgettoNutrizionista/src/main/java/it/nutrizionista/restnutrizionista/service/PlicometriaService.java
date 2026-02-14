package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
import jakarta.validation.Valid;

@Service
public class PlicometriaService {
	
	@Autowired private PlicometriaCalcoliService calcoliService;

    @Autowired private PlicometriaRepository repo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private OwnershipValidator ownershipValidator;

    @Transactional
    public PlicometriaDto create(@Valid PlicometriaFormDto form) {
        Cliente cliente = ownershipValidator.getOwnedCliente(form.getCliente().getId());

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
        if (form.getId() == null) {
            throw new RuntimeException("Id Plicometria obbligatorio per update");
        }

        Plicometria p = ownershipValidator.getOwnedPlicometria(form.getId());

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
        Plicometria p = ownershipValidator.getOwnedPlicometria(id);

        repo.deleteById(id); 
    }

    @Transactional(readOnly = true)
    public PageResponse<PlicometriaDto> allPlicometrieByCliente(Long clienteId, Pageable pageable) {
        ownershipValidator.getOwnedCliente(clienteId);

        // Recupero paginato ordinato per data decrescente
        return PageResponse.from(
            repo.findByCliente_IdOrderByDataMisurazioneDesc(clienteId, pageable)
                .map(DtoMapper::toPlicometriaDto)
        );
    }
}
