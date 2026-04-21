package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.CopyBulkRequest;
import it.nutrizionista.restnutrizionista.dto.CopyBulkResultDto;
import it.nutrizionista.restnutrizionista.dto.CopyDayRequest;
import it.nutrizionista.restnutrizionista.dto.CopyResultItemDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.GiornoSettimana;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoNomeOverrideRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import jakarta.validation.Valid;

@Service
public class SchedaService {

	@Autowired private SchedaRepository repo;
	@Autowired private ClinicalEngineService clinicalEngineService;
	@Autowired private PastoRepository repoPasto;
	@Autowired private AlimentoAlternativoRepository repoAlternative;
	@Autowired private OwnershipValidator ownershipValidator;
	@Autowired private DefaultMealTimesService defaultMealTimesService;
	@Autowired private AlimentoPastoRepository repoAlimentoPasto;
	@Autowired private AlimentoPastoNomeOverrideRepository repoNomeOverride;
	@Autowired private CurrentUserService currentUserService;
	
	@Transactional(readOnly = true)
	public long countAttive() {
		return repo.countByAttivaTrueAndCliente_Nutrizionista_Id(currentUserService.getMe().getId());
	}
	

	@Transactional
	public SchedaDto create(@Valid SchedaFormDto form) {

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

	// Imposta il tipo dalla request, default GIORNALIERA
	if (form.getTipo() != null) {
		try {
			s.setTipo(it.nutrizionista.restnutrizionista.entity.TipoScheda.valueOf(form.getTipo()));
		} catch (IllegalArgumentException e) {
			s.setTipo(it.nutrizionista.restnutrizionista.entity.TipoScheda.GIORNALIERA);
		}
	}

	s.setCliente(cliente);
		Scheda saved = repo.save(s);
		ensureDefaultMeals(saved);
		SchedaDto dto = DtoMapper.toSchedaDtoLight(saved);
		dto.setPasti(repoPasto.findByScheda_IdOrderByOrdineVisualizzazioneAscIdAsc(saved.getId()).stream()
				.map(DtoMapper::toPastoDtoLight)
				.collect(Collectors.toList()));
		return dto;
	}

	private void ensureDefaultMeals(Scheda scheda) {
	String[] defaults = new String[] { "Colazione", "Pranzo", "Merenda", "Cena" };
	List<Pasto> pastiToSave = new java.util.ArrayList<>();

	if (it.nutrizionista.restnutrizionista.entity.TipoScheda.SETTIMANALE.equals(scheda.getTipo())) {
		// Per schede settimanali: crea pasti default per ogni giorno
		for (it.nutrizionista.restnutrizionista.entity.GiornoSettimana giorno : it.nutrizionista.restnutrizionista.entity.GiornoSettimana.values()) {
			for (String nome : defaults) {
				Pasto p = new Pasto();
				p.setScheda(scheda);
				p.setNome(nome);
				p.setDefaultCode(nome);
				p.setEliminabile(false);
				p.setGiorno(giorno);
				if ("Colazione".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(1);
				else if ("Pranzo".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(2);
				else if ("Merenda".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(3);
				else if ("Cena".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(4);
				else p.setOrdineVisualizzazione(999);
				defaultMealTimesService.applyDefaultTimesIfMissing(p);
				pastiToSave.add(p);
			}
		}
	} else {
		// Per schede giornaliere: comportamento originale
		for (String nome : defaults) {
			if (repoPasto.existsByScheda_IdAndDefaultCodeIgnoreCase(scheda.getId(), nome)) continue;
			Pasto p = new Pasto();
			p.setScheda(scheda);
			p.setNome(nome);
			p.setDefaultCode(nome);
			p.setEliminabile(false);
			if ("Colazione".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(1);
			else if ("Pranzo".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(2);
			else if ("Merenda".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(3);
			else if ("Cena".equalsIgnoreCase(nome)) p.setOrdineVisualizzazione(4);
			else p.setOrdineVisualizzazione(999);
			defaultMealTimesService.applyDefaultTimesIfMissing(p);
			pastiToSave.add(p);
		}
	}

	if (!pastiToSave.isEmpty()) {
		repoPasto.saveAll(pastiToSave);
	}
}
	@Transactional
	public SchedaDto update(SchedaFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id scheda obbligatorio per update");
		Scheda s = ownershipValidator.getOwnedScheda(form.getId());
		DtoMapper.updateSchedaFromForm(s, form);
		return DtoMapper.toSchedaDtoLight(repo.save(s));
	}

	/**
	 * Eliminazione ottimizzata: bulk DELETE native SQL in ordine FK
	 * (figli prima, genitori dopo) al posto del cascade Hibernate
	 * che genera N DELETE individuali + N SELECT per lazy loading.
	 *
	 * Ordine: alternative → nome_override → alimenti_pasto → pasti → scheda
	 */
	@Transactional
	public void delete(Long id) {
		if (id == null) throw new RuntimeException("Id scheda obbligatorio per il delete");
		// Verifica ownership (senza caricare l'albero)
		Scheda s = ownershipValidator.getOwnedScheda(id);

		// 1. Elimina alternative (dipendono da alimenti_pasto E pasti)
		repoAlternative.bulkDeleteBySchedaId(id);
		// 2. Elimina nome_override (dipendono da alimenti_pasto)
		repoNomeOverride.bulkDeleteBySchedaId(id);
		// 3. Elimina alimenti_pasto (dipendono da pasti)
		repoAlimentoPasto.bulkDeleteBySchedaId(id);
		// 4. Elimina pasti (dipendono da scheda)
		repoPasto.bulkDeleteBySchedaId(id);
		// 5. Elimina la scheda
		repo.deleteById(id);
	}

	@Transactional(readOnly = true)
	public SchedaDto getById(Long id) {
		Scheda s = ownershipValidator.getOwnedSchedaFullDetails(id);
		return DtoMapper.toSchedaDto(s);
	}

	@Transactional(readOnly = true)
	public PageResponse<SchedaDto> schedeByCliente(Long clienteId, Pageable pageable) {
        ownershipValidator.getOwnedCliente(clienteId);
	    Page<Scheda> page = repo.findByCliente_IdOrderByDataCreazioneDescIdDesc(clienteId, pageable);
	    Page<SchedaDto> dtoPage = page.map(DtoMapper::toSchedaDtoLista);
	    return PageResponse.from(dtoPage);
	}

	// ============================================================
	// COPY BULK — Orchestratore unificato per duplicate e import
	// ============================================================

	/**
	 * Copia bulk della scheda su N clienti.
	 *
	 * NOTA TRANSAZIONALE: l'intera operazione è in un'unica transazione.
	 * - I conflitti di sicurezza (alimenti da evitare) sono gestiti application-level con `continue`,
	 *   quindi non causano rollback.
	 * - Se un errore imprevisto interrompe il loop (es. eccezione DB non attesa), l'intera
	 *   transazione viene annullata — scelta intenzionale per garantire consistenza.
	 * - Se in futuro si necessita di commit parziali (ogni copia indipendente), estrarre la singola
	 *   copia in un metodo @Transactional(propagation = REQUIRES_NEW) su un bean separato.
	 */
	@Transactional
	public CopyBulkResultDto copyBulk(Long schedaId, CopyBulkRequest request) {
		Scheda originale = ownershipValidator.getOwnedSchedaFullDetails(schedaId);
		Long sourceClienteId = originale.getCliente().getId();

		CopyBulkResultDto result = new CopyBulkResultDto();
		result.setTotaleRichiesti(request.getTargetClienteIds().size());

		for (Long targetId : request.getTargetClienteIds()) {
			Cliente targetCliente = ownershipValidator.getOwnedCliente(targetId);
			String nomeCompleto = (targetCliente.getNome() + " " + targetCliente.getCognome()).trim();

			if (targetId.equals(sourceClienteId)) {
				// Duplica per lo stesso cliente — nessun safety check
				SchedaDto cloneDto = duplicateForSameCliente(originale);
				result.addSuccesso(CopyResultItemDto.successo(targetId, nomeCompleto, cloneDto.getId(), true));
			} else {
				// Import su altro cliente — verifica conflitti
				if (!request.isForce()) {
					List<String> conflitti = findSafetyConflicts(originale, targetId);
					if (!conflitti.isEmpty()) {
						result.addConflitto(CopyResultItemDto.conflitto(targetId, nomeCompleto, conflitti));
						continue;
					}
				}
				SchedaDto cloneDto = duplicateToCliente(originale, targetCliente);
				result.addSuccesso(CopyResultItemDto.successo(targetId, nomeCompleto, cloneDto.getId(), false));
			}
		}

		return result;
	}

	// ============================================================
	// Metodi privati di clonazione — usati solo da copyBulk()
	// ============================================================

	/** Duplica la scheda per lo stesso cliente (clone rapido) */
	private SchedaDto duplicateForSameCliente(Scheda originale) {
		Scheda clone = new Scheda();
		clone.setCliente(originale.getCliente());
		clone.setAttiva(false);
		clone.setNome(originale.getNome() + " (Copia)");
		clone.setDataCreazione(LocalDate.now());
		clone.setTipo(originale.getTipo());
		clone.setPasti(clonePastiList(originale.getPasti(), clone, null));
		Scheda savedClone = repo.save(clone);
		deepCloneAlternatives(originale.getPasti(), savedClone);
		return DtoMapper.toSchedaDtoLight(savedClone);
	}

	/** Importa la scheda su un altro cliente (senza safety check — già verificato dal chiamante) */
	private SchedaDto duplicateToCliente(Scheda originale, Cliente targetCliente) {
		Scheda clone = new Scheda();
		clone.setCliente(targetCliente);
		clone.setNome(originale.getNome());
		clone.setDataCreazione(LocalDate.now());
		clone.setAttiva(false);
		clone.setTipo(originale.getTipo());
		clone.setPasti(clonePastiList(originale.getPasti(), clone, null));
		Scheda savedClone = repo.save(clone);
		deepCloneAlternatives(originale.getPasti(), savedClone);
		return DtoMapper.toSchedaDtoLight(savedClone);
	}

	// ============================================================
	// Endpoint legacy — mantenuti per retrocompatibilità
	// ============================================================

	@Transactional
	public SchedaDto duplicateScheda(Long schedaId) {
		Scheda originale = ownershipValidator.getOwnedSchedaWithPastiAndAlimenti(schedaId);
		return duplicateForSameCliente(originale);
	}

	@Transactional
	public SchedaDto duplicateFromCliente(Long schedaId, Long targetClienteId) {
		Scheda originale = ownershipValidator.getOwnedSchedaWithPastiAndAlimenti(schedaId);
		Cliente targetCliente = ownershipValidator.getOwnedCliente(targetClienteId);
		checkSafetyRestrictions(originale, targetClienteId);
		return duplicateToCliente(originale, targetCliente);
	}

    @Transactional
    public SchedaDto copyDay(Long schedaId, CopyDayRequest request) {
        Scheda scheda = ownershipValidator.getOwnedSchedaWithPastiAndAlimenti(schedaId);
        
        // Estrai i pasti del giorno sorgente
        List<Pasto> sourcePasti = scheda.getPasti().stream()
            .filter(p -> request.getSourceDay().equals(p.getGiorno()))
            .collect(Collectors.toList());
            
        if (sourcePasti.isEmpty()) {
            return DtoMapper.toSchedaDto(scheda); // Niente da copiare orginalmente
        }
        
        List<Long> selectedIds = request.getAlimentoPastoIds();
        boolean isPartialCopy = selectedIds != null && !selectedIds.isEmpty();
        
        List<Pasto> pastiToSave = new java.util.ArrayList<>();
        
        for (GiornoSettimana targetDay : request.getTargetDays()) {
            if (targetDay.equals(request.getSourceDay())) continue;
            
            for (Pasto sourcePasto : sourcePasti) {
                // Filtra gli AlimentoPasto del pasto sorgente
                Set<AlimentoPasto> alimentiToCopy = sourcePasto.getAlimentiPasto();
                if (isPartialCopy) {
                    alimentiToCopy = alimentiToCopy.stream()
                        .filter(ap -> selectedIds.contains(ap.getId()))
                        .collect(Collectors.toSet());
                }
                
                // Se questo pasto sorgente non ha alimenti selezionati, lo ignoriamo (non tocchiamo il target)
                if (alimentiToCopy.isEmpty() && isPartialCopy) {
                    continue; 
                }
                
                // Cerca il pasto corrispondente nel giorno target (stesso nome es. "Colazione")
                Pasto targetPasto = scheda.getPasti().stream()
                    .filter(p -> targetDay.equals(p.getGiorno()) && p.getNome().equals(sourcePasto.getNome()))
                    .findFirst()
                    .orElse(null);
                    
                if (targetPasto != null && targetPasto.getId() != null) {
                    // PULIZIA CHIRURGICA SUL DB per evitare Constraint Violations sulle FK
                    repoAlternative.deleteByPasto_Id(targetPasto.getId());
                    repoAlimentoPasto.deleteByPasto_Id(targetPasto.getId());
                    
                    // FORZIAMO IL FLUSH PER SINCRONIZZARE IL DATABASE
                    repoAlternative.flush();
                    repoAlimentoPasto.flush();
                    
                    // Svuotiamo la collezione in memoria (il flush ha già cancellato dal DB)
                    targetPasto.getAlimentiPasto().clear();
                } else if (targetPasto == null) {
                    targetPasto = new Pasto();
                    targetPasto.setNome(sourcePasto.getNome());
                    targetPasto.setDefaultCode(sourcePasto.getDefaultCode());
                    targetPasto.setDescrizione(sourcePasto.getDescrizione());
                    targetPasto.setOrdineVisualizzazione(sourcePasto.getOrdineVisualizzazione());
                    targetPasto.setEliminabile(sourcePasto.getEliminabile());
                    targetPasto.setOrarioInizio(sourcePasto.getOrarioInizio());
                    targetPasto.setOrarioFine(sourcePasto.getOrarioFine());
                    targetPasto.setGiorno(targetDay);
                    targetPasto.setScheda(scheda);
                    scheda.getPasti().add(targetPasto);
                }
                
                Pasto savedTargetPasto = repoPasto.save(targetPasto);
                
                // Clona i nuovi alimenti selezionati dentro il pasto target
                Set<AlimentoPasto> cloniAlimenti = new LinkedHashSet<>();
                for (AlimentoPasto apOriginale : alimentiToCopy) {
                    AlimentoPasto apNuovo = new AlimentoPasto();
                    apNuovo.setAlimento(apOriginale.getAlimento()); 
                    apNuovo.setQuantita(apOriginale.getQuantita()); 
                    apNuovo.setPasto(savedTargetPasto);
                    // Clona il nome custom se presente
                    if (apOriginale.getNomeOverride() != null) {
                        AlimentoPastoNomeOverride nuovoOverride = new AlimentoPastoNomeOverride();
                        nuovoOverride.setNomeCustom(apOriginale.getNomeOverride().getNomeCustom());
                        nuovoOverride.setAlimentoPasto(apNuovo);
                        apNuovo.setNomeOverride(nuovoOverride);
                    }
                    
                    AlimentoPasto savedApNuovo = repoAlimentoPasto.save(apNuovo);
                    cloniAlimenti.add(savedApNuovo);
                    
                    // Clona direttamente le alternative di questo AlimentoPasto specifico
                    List<AlimentoAlternativo> altOriginali = repoAlternative.findByAlimentoPasto_IdOrderByPrioritaAsc(apOriginale.getId());
                    for (AlimentoAlternativo altOrig : altOriginali) {
                        AlimentoAlternativo altNuovo = new AlimentoAlternativo();
                        altNuovo.setAlimentoPasto(savedApNuovo);
                        altNuovo.setPasto(savedTargetPasto);
                        altNuovo.setAlimentoAlternativo(altOrig.getAlimentoAlternativo());
                        altNuovo.setQuantita(altOrig.getQuantita());
                        altNuovo.setPriorita(altOrig.getPriorita());
                        altNuovo.setMode(altOrig.getMode());
                        altNuovo.setManual(altOrig.getManual());
                        altNuovo.setNote(altOrig.getNote());
                        altNuovo.setNomeCustom(altOrig.getNomeCustom());
                        repoAlternative.save(altNuovo);
                    }
                }
                savedTargetPasto.getAlimentiPasto().addAll(cloniAlimenti);
                pastiToSave.add(savedTargetPasto);
            }
        }
        
        repoPasto.saveAll(pastiToSave);
        return DtoMapper.toSchedaDtoLight(scheda);
    }

	/** Logica di clonazione profonda dei pasti e degli alimenti */
    private Set<Pasto> clonePastiList(Set<Pasto> sourcePasti, Scheda targetScheda, GiornoSettimana overrideGiorno) {
        if (sourcePasti == null) return new LinkedHashSet<>();

        return sourcePasti.stream().map(pastoOriginale -> {
            Pasto nuovoPasto = new Pasto();
            nuovoPasto.setNome(pastoOriginale.getNome());
            nuovoPasto.setDefaultCode(pastoOriginale.getDefaultCode());
            nuovoPasto.setDescrizione(pastoOriginale.getDescrizione());
            nuovoPasto.setOrdineVisualizzazione(pastoOriginale.getOrdineVisualizzazione());
            nuovoPasto.setEliminabile(pastoOriginale.getEliminabile());
            nuovoPasto.setOrarioInizio(pastoOriginale.getOrarioInizio());
            nuovoPasto.setOrarioFine(pastoOriginale.getOrarioFine());
            nuovoPasto.setGiorno(overrideGiorno != null ? overrideGiorno : pastoOriginale.getGiorno());
            nuovoPasto.setScheda(targetScheda); // Collega alla nuova scheda

            if (pastoOriginale.getAlimentiPasto() != null) {
                Set<AlimentoPasto> nuoviAlimenti = pastoOriginale.getAlimentiPasto().stream().map(apOriginale -> {
                    AlimentoPasto apNuovo = new AlimentoPasto();
                    apNuovo.setAlimento(apOriginale.getAlimento()); 
                    apNuovo.setQuantita(apOriginale.getQuantita()); 
                    apNuovo.setPasto(nuovoPasto);
                    // Clona il nome custom se presente
                    if (apOriginale.getNomeOverride() != null) {
                        AlimentoPastoNomeOverride nuovoOverride = new AlimentoPastoNomeOverride();
                        nuovoOverride.setNomeCustom(apOriginale.getNomeOverride().getNomeCustom());
                        nuovoOverride.setAlimentoPasto(apNuovo);
                        apNuovo.setNomeOverride(nuovoOverride);
                    }
                    return apNuovo;
                }).collect(Collectors.toCollection(LinkedHashSet::new));
                nuovoPasto.setAlimentiPasto(nuoviAlimenti);
            }
            return nuovoPasto;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Dopo aver salvato la scheda clonata, clona anche le alternative per ogni alimento pasto.
     * Deve essere chiamato DOPO repo.save(clone) per avere gli ID assegnati.
     *
     * OTTIMIZZATO: una sola query bulk (IN clause) + saveAll() batch
     * al posto di N query individuali + N save() singoli.
     */
    private void deepCloneAlternatives(Set<Pasto> sourcePasti, Scheda savedClone) {
        if (sourcePasti == null || savedClone.getPasti() == null) return;

        // 1. Raccoglie tutti gli alimentoPasto.id originali
        List<Long> allSourceApIds = sourcePasti.stream()
                .filter(p -> p.getAlimentiPasto() != null)
                .flatMap(p -> p.getAlimentiPasto().stream())
                .map(AlimentoPasto::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (allSourceApIds.isEmpty()) return;

        // 2. Una sola query bulk: carica TUTTE le alternative con JOIN FETCH
        List<AlimentoAlternativo> allAlternatives = repoAlternative.findAllByAlimentoPastoIds(allSourceApIds);
        if (allAlternatives.isEmpty()) return;

        // 3. Raggruppa per alimentoPasto.id originale → O(1) lookup
        Map<Long, List<AlimentoAlternativo>> altByApId = allAlternatives.stream()
                .collect(Collectors.groupingBy(aa -> aa.getAlimentoPasto().getId()));

        // 4. Itera e crea i cloni, collezionandoli per il batch insert
        List<AlimentoAlternativo> allClones = new ArrayList<>();

        for (Pasto pastoOriginale : sourcePasti) {
            if (pastoOriginale.getAlimentiPasto() == null) continue;

            for (AlimentoPasto apOriginale : pastoOriginale.getAlimentiPasto()) {
                List<AlimentoAlternativo> altOriginali = altByApId.getOrDefault(apOriginale.getId(), Collections.emptyList());
                if (altOriginali.isEmpty()) continue;

                AlimentoPasto apClonato = findClonedAlimentoPasto(savedClone, pastoOriginale, apOriginale);
                if (apClonato == null) continue;

                Pasto pastoClonato = apClonato.getPasto();

                for (AlimentoAlternativo altOrig : altOriginali) {
                    AlimentoAlternativo altNuovo = new AlimentoAlternativo();
                    altNuovo.setAlimentoPasto(apClonato);
                    altNuovo.setPasto(pastoClonato);
                    altNuovo.setAlimentoAlternativo(altOrig.getAlimentoAlternativo());
                    altNuovo.setQuantita(altOrig.getQuantita());
                    altNuovo.setPriorita(altOrig.getPriorita());
                    altNuovo.setMode(altOrig.getMode());
                    altNuovo.setManual(altOrig.getManual());
                    altNuovo.setNote(altOrig.getNote());
                    altNuovo.setNomeCustom(altOrig.getNomeCustom());
                    allClones.add(altNuovo);
                }
            }
        }

        // 5. Batch insert: Hibernate raggruppa in blocchi di batch_size=30
        if (!allClones.isEmpty()) {
            repoAlternative.saveAll(allClones);
        }
    }

    /** Trova l'AlimentoPasto clonato che corrisponde all'originale */
    private AlimentoPasto findClonedAlimentoPasto(Scheda savedClone, Pasto pastoOriginale, AlimentoPasto apOriginale) {
        for (Pasto pClonato : savedClone.getPasti()) {
            boolean matchPasto = pClonato.getDefaultCode() != null 
                ? pClonato.getDefaultCode().equals(pastoOriginale.getDefaultCode())
                : pClonato.getNome().equals(pastoOriginale.getNome());

            // Controlla anche il giorno per schede settimanali:
            // senza questo check, "Colazione Lunedì" matcha anche "Colazione Martedì"
            // causando duplicati nel vincolo uk_pasto_alternativo
            boolean matchGiorno = java.util.Objects.equals(
                pClonato.getGiorno(), pastoOriginale.getGiorno());
            
            if (matchPasto && matchGiorno && pClonato.getAlimentiPasto() != null) {
                for (AlimentoPasto apClonato : pClonato.getAlimentiPasto()) {
                    if (apClonato.getAlimento() != null && apOriginale.getAlimento() != null
                        && java.util.Objects.equals(apClonato.getAlimento().getId(), apOriginale.getAlimento().getId())) {
                        return apClonato;
                    }
                }
            }
        }
        return null;
    }

    /** Restituisce la lista dei nomi degli alimenti in conflitto (vuota se nessun conflitto) */
    private List<String> findSafetyConflicts(Scheda source, Long targetClienteId) {
        List<AlimentoBase> foodInScheda = source.getPasti().stream()
                .flatMap(p -> p.getAlimentiPasto().stream())
                .map(ap -> ap.getAlimento())
                .distinct()
                .collect(Collectors.toList());

        if (foodInScheda.isEmpty()) return new ArrayList<>();

        Cliente target = ownershipValidator.getOwnedCliente(targetClienteId);
        List<it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto> results = clinicalEngineService.valutaInBatch(foodInScheda, target);

        List<String> conflitti = new ArrayList<>();
        for (int i=0; i<foodInScheda.size(); i++) {
            if (results.get(i).stato() == it.nutrizionista.restnutrizionista.enums.LivelloAllerta.ALERT_GRAVE) {
                conflitti.add(foodInScheda.get(i).getNome() + " (Rischio Grave, controllare referto)");
            }
        }
        return conflitti;
    }

    /** Logica di controllo sicurezza centralizzata — usata dagli endpoint legacy */
    private void checkSafetyRestrictions(Scheda source, Long targetClienteId) {
        List<String> conflitti = findSafetyConflicts(source, targetClienteId);
        if (!conflitti.isEmpty()) {
            throw new ConflictException(
                "Impossibile importare la scheda. Conflitti con alimenti da evitare.",
                conflitti
            );
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
        return DtoMapper.toSchedaDtoForSave(repo.save(scheda));
    }
}

