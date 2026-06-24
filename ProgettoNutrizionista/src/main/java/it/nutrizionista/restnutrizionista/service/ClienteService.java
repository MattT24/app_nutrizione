package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
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
		// Controllo duplicati su vincoli univoci, con messaggi chiari (409 Conflict)
		if (repo.existsByCodiceFiscale(form.getCodiceFiscale())) {
			throw new ConflictException("Esiste già un cliente con questo codice fiscale");
		}
		if (repo.existsByEmail(form.getEmail())) {
			throw new ConflictException("Esiste già un cliente con questa email");
		}
		Cliente c = DtoMapper.toCliente(form);
		c.setNutrizionista(u);
		return DtoMapper.toClienteDtoLight(repo.save(c));
	}


	@Transactional
	public ClienteDto update(@Valid ClienteFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id cliente obbligatorio per update");
		Cliente c = ownershipValidator.getOwnedCliente(form.getId());
		// Controllo duplicati escludendo il cliente stesso
		if (repo.existsByCodiceFiscaleAndIdNot(form.getCodiceFiscale(), form.getId())) {
			throw new ConflictException("Esiste già un cliente con questo codice fiscale");
		}
		if (repo.existsByEmailAndIdNot(form.getEmail(), form.getId())) {
			throw new ConflictException("Esiste già un cliente con questa email");
		}
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
		List<Cliente> clienti = repo.findByNutrizionista_Id(u.getId());

		// Prossimo appuntamento per cliente: UNA sola query (no N+1). La lista arriva
		// già ordinata per data/ora, quindi la prima occorrenza per cliente è la più vicina.
		Map<Long, LocalDateTime> prossimoByCliente = new HashMap<>();
		for (Appuntamento a : appuntamentoRepository.findByNutrizionistaIdAndStatoAndDataGreaterThanEqualOrderByDataAscOraAsc(
				u.getId(), Appuntamento.StatoAppuntamento.PRENOTATO, LocalDate.now())) {
			Cliente cli = a.getCliente();
			if (cli == null || cli.getId() == null) continue;
			prossimoByCliente.computeIfAbsent(cli.getId(), k ->
					a.getData().atTime(a.getOra() != null ? a.getOra() : LocalTime.MIDNIGHT));
		}

		return clienti.stream()
				.map(c -> DtoMapper.toClienteLightDto(c, prossimoByCliente.get(c.getId())))
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
