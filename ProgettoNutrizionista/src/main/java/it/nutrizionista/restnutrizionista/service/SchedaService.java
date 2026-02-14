package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoDaEvitareRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import jakarta.validation.Valid;

@Service
public class SchedaService {

	@Autowired private SchedaRepository repo;
	@Autowired private ClienteRepository repoCliente;
	@Autowired private AlimentoDaEvitareRepository repoRestrizioni;
	@Autowired private CurrentUserService currentUserService;
	@Autowired private OwnershipValidator ownershipValidator;
	
	private Utente getMe() { return currentUserService.getMe(); }

	@Transactional
	public SchedaDto create(@Valid SchedaFormDto form) {
		Utente me = getMe();
		if (form.getId() != null) throw new RuntimeException("Id non richiesto per create");
		Cliente cliente = ownershipValidator.getOwnedCliente(form.getCliente().getId());
		
		// Determina se la nuova scheda sarà attiva (default true se null)
		boolean nuovaSchedaAttiva = form.getAttiva() == null || Boolean.TRUE.equals(form.getAttiva());
        
		// Se la nuova scheda sarà attiva, disattiva tutte le altre del cliente
		if (nuovaSchedaAttiva) {
			List<Scheda> schedeAttive = repo.findByCliente_IdAndAttivaTrue(cliente.getId());
			for (Scheda vecchia : schedeAttive) {
	            vecchia.setAttiva(false);
	        }
			repo.saveAll(schedeAttive);
		}

		Scheda s = new Scheda();
		s.setNome(form.getNome()); 
        s.setDataCreazione(LocalDate.now());
		s.setAttiva(nuovaSchedaAttiva);	
		s.setCliente(cliente);
		return DtoMapper.toSchedaDtoLight(repo.save(s));
	}

	@Transactional
	public SchedaDto update(SchedaFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id scheda obbligatorio per update");
		Scheda s = ownershipValidator.getOwnedScheda(form.getId());
		DtoMapper.updateSchedaFromForm(s, form);
		return DtoMapper.toSchedaDtoLight(repo.save(s));
	}

	@Transactional
	public void delete(Long id) {
		if (id == null) throw new RuntimeException("Id scheda obbligatorio per il delete");
		Scheda s = ownershipValidator.getOwnedScheda(id);
		repo.delete(s);
	}

	@Transactional(readOnly = true)
	public SchedaDto getById(Long id) {
		Scheda s = ownershipValidator.getOwnedScheda(id);
		return DtoMapper.toSchedaDto(s);
	}

	@Transactional(readOnly = true)
	public PageResponse<SchedaDto> schedeByCliente(Long clienteId, Pageable pageable) {
        ownershipValidator.getOwnedCliente(clienteId);
	    Page<Scheda> page = repo.findByCliente_IdOrderByDataCreazioneDescIdDesc(clienteId, pageable);
	    Page<SchedaDto> dtoPage = page.map(DtoMapper::toSchedaDtoLista);
	    return PageResponse.from(dtoPage);
	}

	@Transactional
	public SchedaDto duplicateScheda(Long schedaId) {
		Scheda originale = ownershipValidator.getOwnedSchedaWithPastiAndAlimenti(schedaId);

		// Crea il guscio della nuova scheda
		Scheda clone = new Scheda();
		clone.setCliente(originale.getCliente());
		clone.setAttiva(false); // Nasce disattivata per sicurezza
		clone.setNome(originale.getNome()+ " (Copia)"); 
        clone.setDataCreazione(LocalDate.now());
		// Copia dei Pasti
		clone.setPasti(clonePastiList(originale.getPasti(), clone));
		return DtoMapper.toSchedaDtoLight(repo.save(clone));
	}

	@Transactional
    public SchedaDto duplicateFromCliente(Long schedaId, Long targetClienteId) {
        
		Scheda originale = ownershipValidator.getOwnedSchedaWithPastiAndAlimenti(schedaId);
        Cliente targetCliente = ownershipValidator.getOwnedCliente(targetClienteId);

        // 2. CONTROLLO SICUREZZA (Solo qui!)
        checkSafetyRestrictions(originale, targetClienteId);

        // 3. Prepara il clone (Nuovo Cliente)
        Scheda clone = new Scheda();
        clone.setCliente(targetCliente); // <--- Nuovo cliente
        clone.setNome(originale.getNome()); 
        clone.setDataCreazione(LocalDate.now());
        clone.setAttiva(false);

        // 4. Copia i pasti (uso metodo helper)
        clone.setPasti(clonePastiList(originale.getPasti(), clone));

        return DtoMapper.toSchedaDto(repo.save(clone));
    }
	/** Logica di clonazione profonda dei pasti e degli alimenti */
    private List<Pasto> clonePastiList(List<Pasto> sourcePasti, Scheda targetScheda) {
        if (sourcePasti == null) return new ArrayList<>();

        return sourcePasti.stream().map(pastoOriginale -> {
            Pasto nuovoPasto = new Pasto();
            nuovoPasto.setNome(pastoOriginale.getNome());
            nuovoPasto.setOrarioInizio(pastoOriginale.getOrarioInizio());
            nuovoPasto.setOrarioFine(pastoOriginale.getOrarioFine());
            nuovoPasto.setScheda(targetScheda); // Collega alla nuova scheda

            if (pastoOriginale.getAlimentiPasto() != null) {
                List<AlimentoPasto> nuoviAlimenti = pastoOriginale.getAlimentiPasto().stream().map(apOriginale -> {
                    AlimentoPasto apNuovo = new AlimentoPasto();
                    apNuovo.setAlimento(apOriginale.getAlimento()); 
                    apNuovo.setQuantita(apOriginale.getQuantita()); 
                    apNuovo.setPasto(nuovoPasto);
                    return apNuovo;
                }).collect(Collectors.toList());
                nuovoPasto.setAlimentiPasto(nuoviAlimenti);
            }
            return nuovoPasto;
        }).collect(Collectors.toList());
    }

    /** Logica di controllo sicurezza centralizzata */
    private void checkSafetyRestrictions(Scheda source, Long targetClienteId) {
        // Estrai tutti gli ID univoci degli alimenti nella scheda originale
        List<Long> foodIdsInScheda = source.getPasti().stream()
                .flatMap(p -> p.getAlimentiPasto().stream())
                .map(ap -> ap.getAlimento().getId())
                .distinct()
                .collect(Collectors.toList());

        if (foodIdsInScheda.isEmpty()) return;

        // Cerca conflitti nel DB
        List<AlimentoDaEvitare> conflitti = repoRestrizioni.findByCliente_IdAndAlimento_IdIn(targetClienteId, foodIdsInScheda);

        if (!conflitti.isEmpty()) {
            StringBuilder sb = new StringBuilder("Impossibile importare la scheda. Conflitti con alimenti da evitare:\n");
            for (AlimentoDaEvitare c : conflitti) {
                sb.append("- ").append(c.getAlimento().getNome())
                  .append(" (").append(c.getTipo()).append(")\n");
            }
            throw new RuntimeException(sb.toString());
        }
    }
    @Transactional
    public SchedaDto activateScheda(Long schedaId) {
        Scheda scheda = ownershipValidator.getOwnedScheda(schedaId);

        // 3. Disattiva TUTTE le schede di quel cliente
        List<Scheda> schedeCliente = repo.findByCliente_Id(scheda.getCliente().getId());
        for (Scheda s : schedeCliente) {
            s.setAttiva(false);
        }

        // 4. Attiva SOLO quella richiesta
        scheda.setAttiva(true);
        
        // 5. Salva (Hibernate gestirà l'update di tutte le schede modificate nella transazione)
        return DtoMapper.toSchedaDto(repo.save(scheda));
    }
}

