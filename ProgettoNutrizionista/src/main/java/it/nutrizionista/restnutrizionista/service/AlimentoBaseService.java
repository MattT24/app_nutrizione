package it.nutrizionista.restnutrizionista.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.ValoreMicroFormDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Micro;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.entity.ValoreMicro;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.MicroRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.repository.UtentePreferitoRepository;
import it.nutrizionista.restnutrizionista.entity.UtentePreferito;
import jakarta.validation.Valid;

@Service
public class AlimentoBaseService {

	@Autowired private AlimentoBaseRepository repo;
	@Autowired private MicroRepository microRepository;
	@Autowired private UtenteRepository utenteRepository;
	@Autowired private UtentePreferitoRepository preferitoRepository;
	@Autowired private ClinicalEngineService clinicalEngineService;
	@Autowired private OwnershipValidator ownershipValidator;

	/** Crea alimento globale (Admin) — createdBy = null */
	@Transactional
	public AlimentoBaseDto create(@Valid AlimentoBaseFormDto form) {
	    AlimentoBase a = DtoMapper.toAlimentoBase(form);
	    a.setCreatedBy(null); // globale
	    applyMicronutrienti(a, form.getMicroNutrienti());
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}

	/** Crea alimento personale (Nutrizionista) — createdBy = utente loggato */
	@Transactional
	public AlimentoBaseDto createPersonale(@Valid AlimentoBaseFormDto form) {
	    AlimentoBase a = DtoMapper.toAlimentoBase(form);
	    a.setCreatedBy(getCurrentUtente());
	    applyMicronutrienti(a, form.getMicroNutrienti());
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}

	@Transactional
	public AlimentoBaseDto update(@Valid AlimentoBaseFormDto form) {
	    if (form.getId() == null) {
	        throw new RuntimeException("Id Alimento obbligatorio per update");
	    }
	    AlimentoBase a = repo.findById(form.getId())
	            .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
	    DtoMapper.updateAlimentoBaseFromForm(a, form);
	    applyMicronutrienti(a, form.getMicroNutrienti());
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}

	/**
	 * Applica i micronutrienti all'alimento con strategia find-or-create.
	 * Se il DTO micro ha un ID → cerca per ID.
	 * Altrimenti cerca per nome (case-insensitive) o crea un nuovo record Micro.
	 */
	private void applyMicronutrienti(AlimentoBase alimento, List<ValoreMicroFormDto> microDtos) {
		alimento.getMicronutrienti().clear();
		if (microDtos == null || microDtos.isEmpty()) return;

		Set<ValoreMicro> nuoviMicro = new HashSet<>();
		for (ValoreMicroFormDto dto : microDtos) {
			if (dto.getMicronutriente() == null || dto.getValore() == null) continue;

			Micro micro;
			if (dto.getMicronutriente().getId() != null) {
				// Fast path: riferimento per ID (es. da autocomplete)
				micro = microRepository.findById(dto.getMicronutriente().getId())
						.orElseThrow(() -> new RuntimeException(
								"Micronutriente con id " + dto.getMicronutriente().getId() + " non trovato"));
			} else {
				// Find-or-create per nome
				String nome = dto.getMicronutriente().getNome();
				if (nome == null || nome.isBlank()) continue;
				micro = microRepository.findByNomeIgnoreCase(nome.trim())
						.orElseGet(() -> {
							Micro nuovo = new Micro();
							nuovo.setNome(nome.trim());
							nuovo.setUnita(dto.getMicronutriente().getUnita() != null
									? dto.getMicronutriente().getUnita() : "mg");
							nuovo.setCategoria(dto.getMicronutriente().getCategoria());
							nuovo.setCreatedAt(Instant.now());
							nuovo.setUpdatedAt(Instant.now());
							return microRepository.save(nuovo);
						});
			}

			ValoreMicro vm = new ValoreMicro();
			vm.setAlimento(alimento);
			vm.setMicronutriente(micro);
			vm.setValore(dto.getValore());
			vm.setCreatedAt(Instant.now());
			vm.setUpdatedAt(Instant.now());
			nuoviMicro.add(vm);
		}
		alimento.getMicronutrienti().addAll(nuoviMicro);
	}

	@Transactional
    public void delete(Long id) { repo.deleteById(id); }

	/** Elimina solo se l'alimento è personale e appartiene all'utente corrente */
	@Transactional
	public void deletePersonale(Long id) {
		AlimentoBase a = repo.findById(id)
				.orElseThrow(() -> new RuntimeException("Alimento non trovato"));
		Utente current = getCurrentUtente();
		if (a.getCreatedBy() == null || !a.getCreatedBy().getId().equals(current.getId())) {
			throw new RuntimeException("Non puoi eliminare questo alimento");
		}
		repo.deleteById(id);
	}

	/** Lista alimenti visibili: globali + propri dell'utente loggato.
	 *  Se clienteId è presente, arricchisce ogni DTO con la valutazione clinica. */
	@Transactional(readOnly = true)
	public PageResponse<AlimentoBaseDto> listAll(Pageable pageable, Long clienteId) {
		Pageable effective = pageable;
		if (pageable.getSort().isUnsorted()) {
			effective = PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by(Sort.Order.asc("nome").ignoreCase())
			);
		}
		Long utenteId = getCurrentUtente().getId();
		var page = repo.findVisibleByUtente(utenteId, effective);

		if (clienteId == null || page.isEmpty()) {
			return PageResponse.from(page.map(DtoMapper::toAlimentoBaseDtoLight));
		}

		// [SECURITY] ownershipValidator lancia NotFoundException se clienteId non appartiene al nutrizionista
		Cliente cliente = ownershipValidator.getOwnedCliente(clienteId);
		List<AlimentoBase> content = page.getContent();
		List<ValutazioneClinicaDto> valutazioni = clinicalEngineService.valutaInBatch(content, cliente);

		var enriched = page.map(a -> {
			AlimentoBaseDto dto = DtoMapper.toAlimentoBaseDtoLight(a);
			int idx = content.indexOf(a);
			if (idx >= 0 && idx < valutazioni.size()) {
				dto.setValutazioneClinica(valutazioni.get(idx));
			}
			return dto;
		});
		return PageResponse.from(enriched);
	}

	@Transactional(readOnly = true)
	public AlimentoBaseDto getById(Long id) {
		return  repo.findById(id).map(DtoMapper::toAlimentoBaseDtoLight).orElseThrow(()-> new RuntimeException("Alimento non trovato"));
	}
	
	@Transactional(readOnly = true)
	public AlimentoBaseDto dettaglio(Long id) {
		return repo.findByIdWithDetails(id).map(DtoMapper::toAlimentoBaseDto).orElseThrow(()-> new RuntimeException("Alimento non trovato"));
	}
	@Transactional(readOnly = true)
	public AlimentoBaseDto dettaglioMacro(Long id) {
		return repo.findById(id).map(DtoMapper::toAlimentoBaseDtoMacro).orElseThrow(()-> new RuntimeException("Alimento non trovato"));
	}
	
	@Transactional(readOnly = true)
	public AlimentoBaseDto getByNome(@Valid String nome) {
		AlimentoBase a = repo.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
		return DtoMapper.toAlimentoBaseDtoLight(a);
	}

	/** Ricerca filtrata per utente.
	 *  Se clienteId è presente, arricchisce ogni DTO con la valutazione clinica MDSS. */
	@Transactional(readOnly = true)
	public List<AlimentoBaseDto> search(String query, Long clienteId) {
	    String normalizedQuery = query == null ? "" : query.trim();
	    Long utenteId = getCurrentUtente().getId();
	    List<AlimentoBase> list = repo.searchByNomeRankedForUser(normalizedQuery, utenteId);

	    // Caso base: nessun clienteId → risposta identica ad oggi (retrocompatibile)
	    if (clienteId == null || list.isEmpty()) {
	        return list.stream()
	                   .map(DtoMapper::toAlimentoBaseDtoLight)
	                   .collect(Collectors.toList());
	    }

	    // [SECURITY] getOwnedCliente lancia NotFoundException se clienteId non appartiene
	    // al nutrizionista loggato → GlobalExceptionHandler → HTTP 404, nessun leakage
	    Cliente cliente = ownershipValidator.getOwnedCliente(clienteId);

	    // Valutazione clinica in batch: pre-fetch blacklist UNA SOLA VOLTA (Anti N+1)
	    List<ValutazioneClinicaDto> valutazioni = clinicalEngineService.valutaInBatch(list, cliente);

	    List<AlimentoBaseDto> risultati = new ArrayList<>();
	    for (int i = 0; i < list.size(); i++) {
	        AlimentoBaseDto dto = DtoMapper.toAlimentoBaseDtoLight(list.get(i));
	        dto.setValutazioneClinica(valutazioni.get(i));
	        risultati.add(dto);
	    }
	    return risultati;
	}

	/** Categorie visibili per l'utente corrente */
	@Transactional(readOnly = true)
	public List<String> getCategorie() {
		Long utenteId = getCurrentUtente().getId();
		return repo.findDistinctCategorieForUser(utenteId);
	}

	/* ==========================================================
	 * PIÙ UTILIZZATI
	 * ========================================================== */

	@Transactional(readOnly = true)
	public List<AlimentoBaseDto> getTopAlimenti(int limit) {
		Long utenteId = getCurrentUtente().getId();
		List<Long> ids = repo.findTopAlimentiIdsByNutrizionista(utenteId, PageRequest.of(0, limit));
		if (ids.isEmpty()) return List.of();
		// Carica entità con macro+tracce in una sola query, poi riordina come gli ID originali
		List<AlimentoBase> entities = repo.findAllByIdInWithMacro(ids);
		Map<Long, AlimentoBase> byId = entities.stream().collect(Collectors.toMap(AlimentoBase::getId, a -> a));
		return ids.stream()
				.map(byId::get)
				.filter(java.util.Objects::nonNull)
				.map(DtoMapper::toAlimentoBaseDtoLight)
				.collect(Collectors.toList());
	}

	/**
	 * [Task 18] Classifica globale degli alimenti più usati su tutta la piattaforma.
	 * Risultato memorizzato in cache Spring ("topAlimentiGlobal") per evitare
	 * Full Table Scan ad ogni richiesta. La cache è valida per l'intera sessione
	 * del server (TTL configurabile aggiungendo Caffeine/Redis in futuro).
	 */
	@Cacheable("topAlimentiGlobal")
	@Transactional(readOnly = true)
	public List<AlimentoBaseDto> getTopAlimentiGlobal(int limit) {
		List<Long> ids = repo.findTopAlimentiIdsGlobal(PageRequest.of(0, limit));
		if (ids.isEmpty()) return List.of();
		List<AlimentoBase> entities = repo.findAllByIdInWithMacro(ids);
		Map<Long, AlimentoBase> byId = entities.stream().collect(Collectors.toMap(AlimentoBase::getId, a -> a));
		return ids.stream()
				.map(byId::get)
				.filter(java.util.Objects::nonNull)
				.map(DtoMapper::toAlimentoBaseDtoLight)
				.collect(Collectors.toList());
	}

	/* ==========================================================
	 * GESTIONE PREFERITI
	 * ========================================================== */

	@Transactional(readOnly = true)
	public List<AlimentoBaseDto> getPreferiti() {
		Long utenteId = getCurrentUtente().getId();
		return preferitoRepository.findByUtenteIdOrderByCreatedAtDesc(utenteId).stream()
				.map(UtentePreferito::getAlimento)
				.map(DtoMapper::toAlimentoBaseDtoLight)
				.collect(Collectors.toList());
	}

	@Transactional
	public void addPreferito(Long alimentoId) {
		Utente current = getCurrentUtente();
		AlimentoBase alimento = repo.findById(alimentoId)
				.orElseThrow(() -> new RuntimeException("Alimento non trovato"));

		if (!preferitoRepository.existsByUtenteIdAndAlimentoId(current.getId(), alimentoId)) {
			UtentePreferito preferito = new UtentePreferito(current, alimento);
			preferitoRepository.save(preferito);
		}
	}

	@Transactional
	public void removePreferito(Long alimentoId) {
		Long utenteId = getCurrentUtente().getId();
		preferitoRepository.deleteByUtenteIdAndAlimentoId(utenteId, alimentoId);
	}

	/** Ottiene l'utente corrente dal SecurityContext (JWT) */
	private Utente getCurrentUtente() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return utenteRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Utente non trovato"));
	}
}

