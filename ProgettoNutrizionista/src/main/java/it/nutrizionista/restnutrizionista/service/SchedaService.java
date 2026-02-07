package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import jakarta.validation.Valid;

@Service
public class SchedaService {

	@Autowired private SchedaRepository repo;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private ClienteRepository repoCliente;
	@Autowired private AlimentoDaEvitareRepository repoRestrizioni;
	
	private Utente getMe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return repoUtente.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
    }
	
	private void checkClienteOwnership(Cliente cliente, Utente nutrizionista) {
        if (!cliente.getNutrizionista().getId().equals(nutrizionista.getId())) {
            throw new SecurityException("NON AUTORIZZATO: Il cliente selezionato non è gestito da te.");
        }
    }

	@Transactional
	public SchedaDto create(@Valid SchedaFormDto form) {
		Utente me = getMe();
		if (form.getId() != null) throw new RuntimeException("Id non richiesto per create");
		Cliente cliente = repoCliente.findById(form.getCliente().getId())
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
		checkClienteOwnership(cliente, me);
		
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
		Utente u =getMe();
		if (form.getId() == null) throw new RuntimeException("Id scheda obbligatorio per update");
		Scheda s = repo.findById(form.getId())
				.orElseThrow(() -> new RuntimeException("Scheda non trovata"));
		checkOwnership(s, u);
		DtoMapper.updateSchedaFromForm(s, form);
		return DtoMapper.toSchedaDtoLight(repo.save(s));
	}

	@Transactional
	public void delete(Long id) {
		Utente u = getMe();
		if (id == null) throw new RuntimeException("Id scheda obbligatorio per il delete");
		Scheda s = repo.findById(id)
				.orElseThrow(() -> new RuntimeException("Scheda non trovata"));
		checkOwnership(s, u);
		repo.deleteById(id);
	}

	@Transactional(readOnly = true)
	public SchedaDto getById(Long id) {
		Utente u = getMe();
		Scheda s = repo.findById(id)
				.orElseThrow(() -> new RuntimeException("Scheda non trovata"));
		checkOwnership(s, u);
		return DtoMapper.toSchedaDto(s);
	}

	@Transactional(readOnly = true)
	public PageResponse<SchedaDto> schedeByCliente(Long clienteId, Pageable pageable) {
		Utente me = getMe();
        Cliente c = repoCliente.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente non trovato"));
        checkClienteOwnership(c, me);
	    Page<Scheda> page = repo.findByCliente_IdOrderByDataCreazioneDesc(clienteId, pageable);
	    Page<SchedaDto> dtoPage = page.map(DtoMapper::toSchedaDtoLista);
	    return PageResponse.from(dtoPage);
	}

	// Metodo helper privato
	private void checkOwnership(Scheda scheda, Utente nutrizionista) {
	    if (!scheda.getCliente().getNutrizionista().getId().equals(nutrizionista.getId())) {
	        throw new SecurityException("NON AUTORIZZATO: Questa scheda appartiene al cliente di un altro professionista.");
	    }
	}


	@Transactional
	public SchedaDto duplicateScheda(Long schedaId) {
		Utente me = getMe();
		//  Carica la scheda originale
		Scheda originale = repo.findByIdWithPastiAndAlimenti(schedaId)
				.orElseThrow(() -> new RuntimeException("Scheda originale non trovata"));

	    checkOwnership(originale, me);

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
        
		Utente me = getMe();
		//  Carica la scheda originale
		Scheda originale = repo.findByIdWithPastiAndAlimenti(schedaId)
				.orElseThrow(() -> new RuntimeException("Scheda originale non trovata"));

	    checkOwnership(originale, me);
        Cliente targetCliente = repoCliente.findById(targetClienteId)
                .orElseThrow(() -> new RuntimeException("Cliente destinazione non trovato"));

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
        // 1. Recupera la scheda e l'utente loggato
        Utente me = getMe(); 
        Scheda scheda = repo.findById(schedaId)
                .orElseThrow(() -> new RuntimeException("Scheda non trovata"));

        // 2. Controllo sicurezza (vedi punto 2 sotto)
        checkOwnership(scheda, me);

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

