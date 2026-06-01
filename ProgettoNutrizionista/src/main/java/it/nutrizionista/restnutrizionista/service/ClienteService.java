package it.nutrizionista.restnutrizionista.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.ClienteInfoDto;
import it.nutrizionista.restnutrizionista.dto.ClienteLightDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PesoAltezzaRequest;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoNomeOverrideRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import jakarta.validation.Valid;

@Service
public class ClienteService {

	@Autowired private ClienteRepository repo;
	@Autowired private CurrentUserService currentUserService;
	@Autowired private OwnershipValidator ownershipValidator;
	@Autowired private CalcoloTdeeRepository calcoloTdeeRepository;
	@Autowired private SchedaRepository schedaRepository;
	@Autowired private PastoRepository pastoRepository;
	@Autowired private AlimentoPastoRepository alimentoPastoRepository;
	@Autowired private AlimentoPastoNomeOverrideRepository alimentoPastoNomeOverrideRepository;
	@Autowired private AlimentoAlternativoRepository alimentoAlternativoRepository;
	@Autowired private AppuntamentoRepository appuntamentoRepository;

	@Transactional
	public ClienteDto create(@Valid ClienteFormDto form) {
		Utente u = currentUserService.getMe();
		//controllo se è già presente un cliente con quel CF
		if(repo.existsByCodiceFiscale(form.getCodiceFiscale())) {
            throw new RuntimeException("Cliente già esistente (CF duplicato)");
       }
		Cliente c = DtoMapper.toCliente(form);
		c.setNutrizionista(u);
		return DtoMapper.toClienteDtoLight(repo.save(c));
	}


	@Transactional
	public ClienteDto update(@Valid ClienteFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id cliente obbligatorio per update");
		Cliente c = ownershipValidator.getOwnedCliente(form.getId());
		DtoMapper.updateClienteFromForm(c, form);
		return DtoMapper.toClienteDto(repo.save(c));
	}

	@Transactional
	public ClienteDto updatePesoAltezza(@Valid PesoAltezzaRequest req) {
		if (req.getId() == null) throw new RuntimeException("Id cliente obbligatorio per update");
		Cliente c = ownershipValidator.getOwnedCliente(req.getId());
		if (req.getPeso() != null) c.setPeso(req.getPeso());
		if (req.getAltezza() != null) c.setAltezza(req.getAltezza());
		// Permettiamo di impostare anche a null il pesoTarget se inviato, se serve
		c.setPesoTarget(req.getPesoTarget());
		return DtoMapper.toClienteDto(repo.save(c));
	}

	@Transactional
	public void deleteMyCliente(Long id) {
	    if (id == null) throw new RuntimeException("Id cliente obbligatorio per il delete");

	    // Verifica ownership (carica solo il cliente, non l'albero delle schede)
	    Cliente c = ownershipValidator.getOwnedCliente(id);

	    // 1. Svuota in modo robusto l'albero profondo di OGNI scheda del cliente.
	    //    Non ci si può affidare al solo cascade ORM: AlimentoAlternativo ha due FK
	    //    (alimento_pasto_id + pasto_id) ma solo alimento_pasto_id è coperta da
	    //    orphanRemoval; con il batching JDBC questo genera StaleStateException
	    //    ("row count 0; expected 1") sulla delete delle alternative.
	    //    Stesso ordine bottom-up di SchedaService.delete().
	    for (Long schedaId : schedaRepository.findIdsByCliente_Id(id)) {
	        alimentoAlternativoRepository.bulkDeleteBySchedaId(schedaId);        // alternative (dipendono da alimenti_pasto E pasti)
	        alimentoPastoNomeOverrideRepository.bulkDeleteBySchedaId(schedaId);  // nome_override
	        alimentoPastoRepository.bulkDeleteBySchedaId(schedaId);              // alimenti_pasto
	        pastoRepository.bulkDeleteBySchedaId(schedaId);                      // pasti
	    }

	    // 2. Appuntamenti: la FK cliente_id NON è in cascade dal Cliente.
	    appuntamentoRepository.deleteByCliente_Id(id);

	    // 3. Storico dei calcoli TDEE associati a questo cliente.
	    calcoloTdeeRepository.deleteByClienteId(id);

	    // 4. Infine il cliente: il cascade ORM gestisce ora solo le collezioni mono-FK
	    //    (schede ormai vuote, misurazioni, plicometrie, obiettivi, blacklist, tag).
	    repo.delete(c);
	}

	@Transactional(readOnly = true)
	public PageResponse<ClienteLightDto> allMyClienti( Pageable pageable) {
		Utente u = currentUserService.getMe();
	    int maxSize = 12;
	    if (pageable.getPageSize() > maxSize) {
	        pageable = PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
	    }
		return PageResponse.from(repo.findByNutrizionista_Id(u.getId(),pageable).map(DtoMapper::toClienteLightDto));
	}
	
	@Transactional(readOnly = true)
	public List<ClienteLightDto> allMyClientiList() {
		Utente u = currentUserService.getMe();
		// Assicurati di avere questo metodo nel tuo ClienteRepository:
		// List<Cliente> findByNutrizionista_Id(Long id);
		return repo.findByNutrizionista_Id(u.getId()).stream()
				.map(DtoMapper::toClienteLightDto)
				.toList();
	}

	@Transactional(readOnly = true)
    public ClienteDto getById(Long id) {
        Cliente c = ownershipValidator.getOwnedCliente(id);
        return DtoMapper.toClienteDtoLight(c);
    }
	
	@Transactional(readOnly = true)
    public List<ClienteDto> findByNome(String nome) {
        Utente me = currentUserService.getMe();
        return repo.findByNutrizionista_IdAndNomeContainingIgnoreCase(me.getId(), nome)
                .stream().map(DtoMapper::toClienteDtoLight).toList();
    }
	@Transactional(readOnly = true)
	public List<ClienteDto> findByCognome(@Valid String cognome) {
        Utente me = currentUserService.getMe();
        return repo.findByNutrizionista_IdAndCognomeContainingIgnoreCase(me.getId(), cognome)
                .stream().map(DtoMapper::toClienteDtoLight).toList();
	}
	
	@Transactional(readOnly = true)
    public ClienteInfoDto dettaglio(Long id) {
        Cliente c = ownershipValidator.getOwnedCliente(id);
        return DtoMapper.toClienteInfoDto(c);
    }
	//manca cliente Fabbisogno, da studiare un attimo
}
