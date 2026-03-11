package it.nutrizionista.restnutrizionista.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Micro;
import it.nutrizionista.restnutrizionista.entity.Utente;
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

	/** Crea alimento globale (Admin) — createdBy = null */
	@Transactional
	public AlimentoBaseDto create(@Valid AlimentoBaseFormDto form) {
	    Map<Long, Micro> microCatalogo = loadMicroCatalogo();
	    AlimentoBase a = DtoMapper.toAlimentoBase(form, microCatalogo);
	    a.setCreatedBy(null); // globale
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}

	/** Crea alimento personale (Nutrizionista) — createdBy = utente loggato */
	@Transactional
	public AlimentoBaseDto createPersonale(@Valid AlimentoBaseFormDto form) {
	    Map<Long, Micro> microCatalogo = loadMicroCatalogo();
	    AlimentoBase a = DtoMapper.toAlimentoBase(form, microCatalogo);
	    a.setCreatedBy(getCurrentUtente());
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
	}

	@Transactional
	public AlimentoBaseDto update(@Valid AlimentoBaseFormDto form) {
	    if (form.getId() == null) {
	        throw new RuntimeException("Id Alimento obbligatorio per update");
	    }
	    AlimentoBase a = repo.findById(form.getId())
	            .orElseThrow(() -> new RuntimeException("Alimento non trovato"));
	    Map<Long, Micro> microCatalogo = loadMicroCatalogo();
	    DtoMapper.updateAlimentoBaseFromForm(a, form, microCatalogo);
	    return DtoMapper.toAlimentoBaseDtoLight(repo.save(a));
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

	/** Lista alimenti visibili: globali + propri dell'utente loggato */
	@Transactional(readOnly = true)
	public PageResponse<AlimentoBaseDto> listAll(Pageable pageable) {
		Pageable effective = pageable;
		if (pageable.getSort().isUnsorted()) {
			effective = PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by(Sort.Order.asc("nome").ignoreCase())
			);
		}
		Long utenteId = getCurrentUtente().getId();
		return PageResponse.from(repo.findVisibleByUtente(utenteId, effective).map(DtoMapper::toAlimentoBaseDtoLight));
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

	/** Ricerca filtrata per utente */
	@Transactional(readOnly = true)
	public List<AlimentoBaseDto> search(String query) {
	    String normalizedQuery = query == null ? "" : query.trim();
	    Long utenteId = getCurrentUtente().getId();
	    List<AlimentoBase> list = repo.searchByNomeRankedForUser(normalizedQuery, utenteId);
	    return list.stream()
	               .map(DtoMapper::toAlimentoBaseDtoLight)
	               .collect(Collectors.toList());
	}

	/** Categorie visibili per l'utente corrente */
	@Transactional(readOnly = true)
	public List<String> getCategorie() {
		Long utenteId = getCurrentUtente().getId();
		return repo.findDistinctCategorieForUser(utenteId);
	}

	private Map<Long, Micro> loadMicroCatalogo() {
	    return microRepository.findAll()
	            .stream()
	            .collect(Collectors.toMap(
	                Micro::getId,
	                Function.identity()
	            ));
	}

	/* ==========================================================
	 * PIÙ UTILIZZATI
	 * ========================================================== */

	@Transactional(readOnly = true)
	public List<AlimentoBaseDto> getTopAlimenti(int limit) {
		Long utenteId = getCurrentUtente().getId();
		return repo.findTopAlimentiByNutrizionista(utenteId, PageRequest.of(0, limit)).stream()
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

