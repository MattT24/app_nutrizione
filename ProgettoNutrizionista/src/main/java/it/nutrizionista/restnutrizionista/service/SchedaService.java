package it.nutrizionista.restnutrizionista.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


	@Transactional
	public SchedaDto create(@Valid SchedaFormDto form) {
		if (form.getId() != null) throw new RuntimeException("Id non richiesto per create");
		if (Boolean.TRUE.equals(form.getAttiva())) {
			List<Scheda> schedeAttive = repo.findByCliente_IdAndAttivaTrue(form.getCliente().getId());
			schedeAttive.forEach(vecchia -> vecchia.setAttiva(false));
			repo.saveAll(schedeAttive);
		}

		Scheda s = new Scheda();
		s.setAttiva(form.getAttiva() != null ? form.getAttiva() : true);	
		s.setCliente(form.getCliente());
		return DtoMapper.toSchedaDtoLight(repo.save(s));
	}

	@Transactional
	public SchedaDto update(SchedaFormDto form) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Utente u = repoUtente.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		if (form.getId() == null) throw new RuntimeException("Id scheda obbligatorio per update");
		Scheda s = repo.findById(form.getId())
				.orElseThrow(() -> new RuntimeException("Scheda non trovata"));
		if (u.getId()!= s.getCliente().getNutrizionista().getId()) throw new RuntimeException("Non autorizzato");
		DtoMapper.updateSchedaFromForm(s, form);
		return DtoMapper.toSchedaDto(repo.save(s));
	}

	@Transactional
	public void delete(Long id) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Utente u = repoUtente.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		if (id == null) throw new RuntimeException("Id scheda obbligatorio per il delete");
		Scheda s = repo.findById(id)
				.orElseThrow(() -> new RuntimeException("Scheda non trovata"));
		if(u.getId()!= s.getCliente().getNutrizionista().getId()) throw new RuntimeException("L'utente non possiede la scheda");
		repo.deleteById(id);
	}

	@Transactional(readOnly = true)
	public SchedaDto getById(Long id) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Utente u = repoUtente.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		Scheda s = repo.findById(id)
				.orElseThrow(() -> new RuntimeException("Scheda non trovata"));
		if (u.getId()!= s.getCliente().getNutrizionista().getId()) throw new RuntimeException("Non autorizzato");
		return DtoMapper.toSchedaDto(s);
	}

	@Transactional(readOnly = true)
	public SchedaDto pastiByScheda(Long id) {
		return  repo.findById(id).map(DtoMapper::toSchedaDtoListaPasti).orElseThrow(()-> new RuntimeException("Scheda non trovata"));
	}

	@Transactional(readOnly = true)
	public List<SchedaDto> schedeByCliente(Long id) {
		Cliente c = repoCliente.findById(id).orElseThrow(()-> new RuntimeException("Cliente non trovato"));
		return c.getSchede().stream().map(DtoMapper::toSchedaDtoListaPasti).collect(Collectors.toList());
	}


	@Transactional
	public SchedaDto duplicateScheda(Long schedaId) {
		//  Carica la scheda originale
		Scheda originale = repo.findByIdWithPastiAndAlimenti(schedaId)
				.orElseThrow(() -> new RuntimeException("Scheda originale non trovata"));

		// Crea il guscio della nuova scheda
		Scheda clone = new Scheda();
		clone.setCliente(originale.getCliente());
		clone.setAttiva(false); // Nasce disattivata per sicurezza

		// Copia dei Pasti
		clone.setPasti(clonePastiList(originale.getPasti(), clone));
		return DtoMapper.toSchedaDto(repo.save(clone));
	}

	@Transactional
    public SchedaDto duplicateFromCliente(Long sourceSchedaId, Long targetClienteId) {
        
        // 1. Carica dati
        Scheda source = repo.findByIdWithPastiAndAlimenti(sourceSchedaId)
                .orElseThrow(() -> new RuntimeException("Scheda sorgente non trovata"));
        
        Cliente targetCliente = repoCliente.findById(targetClienteId)
                .orElseThrow(() -> new RuntimeException("Cliente destinazione non trovato"));

        // 2. CONTROLLO SICUREZZA (Solo qui!)
        checkSafetyRestrictions(source, targetClienteId);

        // 3. Prepara il clone (Nuovo Cliente)
        Scheda clone = new Scheda();
        clone.setCliente(targetCliente); // <--- Nuovo cliente
        clone.setAttiva(false);

        // 4. Copia i pasti (uso metodo helper)
        clone.setPasti(clonePastiList(source.getPasti(), clone));

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
}

