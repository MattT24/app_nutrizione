package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AlimentoPastoSchedaTemplateUpsertDto;
import it.nutrizionista.restnutrizionista.dto.ApplicaSchedaTemplateRequest;
import it.nutrizionista.restnutrizionista.dto.CopyDayRequest;
import it.nutrizionista.restnutrizionista.dto.PastoSchedaTemplateUpsertDto;
import it.nutrizionista.restnutrizionista.dto.SchedaDto;
import it.nutrizionista.restnutrizionista.dto.SchedaFormDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateMetadataPatchDto;
import it.nutrizionista.restnutrizionista.dto.SchedaTemplateUpsertDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.AlimentoSchedaTemplateAlternativa;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.GiornoSettimana;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.PastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.TipoScheda;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoSchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoSchedaTemplateAlternativaRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.PastoSchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaTemplateRepository;
import jakarta.validation.Valid;

@Service
public class SchedaTemplateService {

	@Autowired private SchedaTemplateRepository repo;
	@Autowired private AlimentoBaseRepository alimentoBaseRepository;
	@Autowired private SchedaRepository schedaRepository;
	@Autowired private ClienteRepository clienteRepository;
	@Autowired private CurrentUserService currentUserService;
	@Autowired private DefaultMealTimesService defaultMealTimesService;
	@Autowired private AlimentoSchedaTemplateAlternativaRepository alternativaRepo;
	@Autowired private AlimentoAlternativoRepository alimentoAlternativoRepo;
	@Autowired private PastoSchedaTemplateRepository pastoSchedaTemplateRepo;
	@Autowired private AlimentoPastoSchedaTemplateRepository alimentoPastoSchedaTemplateRepo;
	@Autowired private PastoRepository pastoRepo;
	@Autowired private AlimentoPastoRepository alimentoPastoRepo;

	// ═══════════════════════════════════════════
	// CRUD
	// ═══════════════════════════════════════════

	@Transactional(readOnly = true)
	public List<SchedaTemplateDto> listMine() {
		var me = currentUserService.getMe();
		return repo.findByCreatedByIdWithFullTree(me.getId()).stream()
				.map(DtoMapper::toSchedaTemplateDto)
				.collect(Collectors.toList());
	}

	/** Lista leggera — solo metadati (id, nome, tipo), senza caricare pasti/alimenti */
	@Transactional(readOnly = true)
	public List<SchedaTemplateDto> listMineSummary() {
		var me = currentUserService.getMe();
		return repo.findAllByCreatedByIdOrderByUpdatedAtDesc(me.getId()).stream()
				.map(st -> {
					SchedaTemplateDto dto = new SchedaTemplateDto();
					dto.setId(st.getId());
					dto.setNome(st.getNome());
					dto.setDescrizione(st.getDescrizione());
					dto.setTipo(st.getTipo() != null ? st.getTipo().name() : null);
					dto.setPasti(List.of()); // lista vuota — non serve per la dropdown
					return dto;
				})
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public SchedaTemplateDto getById(Long id) {
		var me = currentUserService.getMe();
		SchedaTemplate st = repo.findByIdWithFullTree(id)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		checkOwnership(st, me.getId());
		return DtoMapper.toSchedaTemplateDto(st);
	}

	@Transactional
	public SchedaTemplateDto create(@Valid SchedaTemplateUpsertDto req) {
		var me = currentUserService.getMe();

		SchedaTemplate st = new SchedaTemplate();
		st.setNome(req.getNome().trim());
		st.setDescrizione(normalizeString(req.getDescrizione()));
		st.setTipo(parseTipoScheda(req.getTipo()));
		st.setCreatedBy(me);
		applyPasti(st, req.getPasti());

		// Se il client non ha inviato pasti (nuovo template blank), inizializza
		// gli stessi pasti di default usati da SchedaService.ensureDefaultMeals().
		if (st.getPasti().isEmpty()) {
			ensureDefaultPastiTemplate(st);
		}

		return DtoMapper.toSchedaTemplateDto(repo.save(st));
	}

	/**
	 * UPDATE legacy (retrocompatibilità) — aggiorna SOLO metadata, ignora i pasti.
	 * Per le modifiche ai pasti usare le API granulari di PastoSchedaTemplateController.
	 */
	@Transactional
	public SchedaTemplateDto update(Long id, @Valid SchedaTemplateUpsertDto req) {
		var me = currentUserService.getMe();
		SchedaTemplate st = repo.findById(id)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		checkOwnership(st, me.getId());

		st.setNome(req.getNome().trim());
		st.setDescrizione(normalizeString(req.getDescrizione()));
		st.setTipo(parseTipoScheda(req.getTipo()));
		// NOTA: i pasti vengono ignorati — gestiti dalle API granulari

		return DtoMapper.toSchedaTemplateDto(repo.save(st));
	}

	/**
	 * PATCH metadata-only (nuovo endpoint preferito).
	 */
	@Transactional
	public SchedaTemplateDto patchMetadata(Long id, @Valid SchedaTemplateMetadataPatchDto dto) {
		var me = currentUserService.getMe();
		SchedaTemplate st = repo.findById(id)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		checkOwnership(st, me.getId());

		st.setNome(dto.nome().trim());
		st.setDescrizione(normalizeString(dto.descrizione()));
		st.setTipo(parseTipoScheda(dto.tipo()));

		return DtoMapper.toSchedaTemplateDto(repo.save(st));
	}

	@Transactional
	public void delete(Long id) {
		var me = currentUserService.getMe();
		SchedaTemplate st = repo.findById(id)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		checkOwnership(st, me.getId());
		repo.delete(st);
	}

	// ═══════════════════════════════════════════
	// APPLICA TEMPLATE SU SCHEDA ESISTENTE
	// ═══════════════════════════════════════════

	@Transactional
	public SchedaDto applicaAScheda(Long templateId, Long schedaId, @Valid ApplicaSchedaTemplateRequest req) {
		var me = currentUserService.getMe();

		SchedaTemplate st = repo.findByIdWithFullTree(templateId)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		checkOwnership(st, me.getId());

		Scheda scheda = schedaRepository.findByIdWithFullDetailsMine(schedaId, me.getId())
				.orElseThrow(() -> new NotFoundException("Scheda non trovata o non accessibile"));

		String mode = req.getMode();
		boolean isMerge = "MERGE".equalsIgnoreCase(mode);
		if ("REPLACE".equalsIgnoreCase(mode)) {
			scheda.getPasti().clear();
		}

		clonaPastiSuScheda(st, scheda, isMerge);
		return DtoMapper.toSchedaDto(scheda);
	}

	// ═══════════════════════════════════════════
	// CREA SCHEDA DA TEMPLATE
	// ═══════════════════════════════════════════

	@Transactional
	public SchedaDto creaSchedaDaTemplate(Long templateId, SchedaFormDto schedaForm) {
		var me = currentUserService.getMe();

		SchedaTemplate st = repo.findByIdWithFullTree(templateId)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		checkOwnership(st, me.getId());

		if (schedaForm.getCliente() == null || schedaForm.getCliente().getId() == null) {
			throw new BadRequestException("Il cliente e' obbligatorio");
		}

		Cliente cliente = clienteRepository.findById(schedaForm.getCliente().getId())
				.orElseThrow(() -> new NotFoundException("Cliente non trovato"));

		Scheda scheda = new Scheda();
		scheda.setNome(schedaForm.getNome() != null ? schedaForm.getNome().trim() : st.getNome());
		scheda.setCliente(cliente);
		scheda.setAttiva(schedaForm.getAttiva() != null ? schedaForm.getAttiva() : true);
		scheda.setDataCreazione(LocalDate.now());
		scheda.setTipo(st.getTipo());

		Scheda savedScheda = schedaRepository.save(scheda);
		clonaPastiSuScheda(st, savedScheda, false);
		return DtoMapper.toSchedaDto(savedScheda);
	}

	// ═══════════════════════════════════════════
	// COPIA GIORNO (template settimanali)
	// ═══════════════════════════════════════════

	/**
	 * Copia i pasti di un giorno sorgente sui giorni target del template.
	 * Se nel giorno target esiste già un pasto con lo stesso nome → MERGE
	 * (svuota e riempie con i cloni del sorgente).
	 * Se non esiste → crea un nuovo pasto.
	 * Logica speculare a SchedaService.copyDay().
	 */
	@Transactional
	public SchedaTemplateDto copyDay(Long templateId, @Valid CopyDayRequest request) {
		var me = currentUserService.getMe();
		SchedaTemplate st = repo.findByIdWithFullTree(templateId)
				.orElseThrow(() -> new NotFoundException("Template scheda non trovato"));
		checkOwnership(st, me.getId());

		// Pasti del giorno sorgente
		List<PastoSchedaTemplate> sourcePasti = st.getPasti().stream()
				.filter(p -> request.getSourceDay().equals(p.getGiorno()))
				.collect(Collectors.toList());

		if (sourcePasti.isEmpty()) {
			return DtoMapper.toSchedaTemplateDto(st);
		}

		List<Long> selectedIds = request.getAlimentoPastoIds();
		boolean isPartialCopy = selectedIds != null && !selectedIds.isEmpty();

		for (GiornoSettimana targetDay : request.getTargetDays()) {
			if (targetDay.equals(request.getSourceDay())) continue;

			for (PastoSchedaTemplate sourcePasto : sourcePasti) {
				// Filtra gli alimenti selezionati
				List<AlimentoPastoSchedaTemplate> alimentiToCopy = sourcePasto.getAlimenti();
				if (isPartialCopy) {
					alimentiToCopy = alimentiToCopy.stream()
							.filter(apt -> selectedIds.contains(apt.getId()))
							.collect(Collectors.toList());
				}

				if (alimentiToCopy.isEmpty() && isPartialCopy) continue;

				// Cerca pasto corrispondente nel giorno target (stesso nome)
				PastoSchedaTemplate targetPasto = st.getPasti().stream()
						.filter(p -> targetDay.equals(p.getGiorno()) && p.getNome().equals(sourcePasto.getNome()))
						.findFirst()
						.orElse(null);

				if (targetPasto != null) {
					// MERGE: svuota gli alimenti esistenti e le loro alternative
					targetPasto.getAlimenti().clear();
				} else {
					// CREATE: nuovo pasto nel giorno target
					targetPasto = new PastoSchedaTemplate();
					targetPasto.setSchedaTemplate(st);
					targetPasto.setNome(sourcePasto.getNome());
					targetPasto.setDescrizione(sourcePasto.getDescrizione());
					targetPasto.setOrdineVisualizzazione(sourcePasto.getOrdineVisualizzazione());
					targetPasto.setOrarioInizio(sourcePasto.getOrarioInizio());
					targetPasto.setOrarioFine(sourcePasto.getOrarioFine());
					targetPasto.setGiorno(targetDay);
					st.getPasti().add(targetPasto);
				}

				PastoSchedaTemplate savedTarget = pastoSchedaTemplateRepo.save(targetPasto);

				// Clona gli alimenti selezionati nel pasto target
				for (AlimentoPastoSchedaTemplate srcApt : alimentiToCopy) {
					AlimentoPastoSchedaTemplate newApt = new AlimentoPastoSchedaTemplate();
					newApt.setPastoSchedaTemplate(savedTarget);
					newApt.setAlimento(srcApt.getAlimento());
					newApt.setQuantita(srcApt.getQuantita());
					newApt.setNomeCustom(srcApt.getNomeCustom());
					AlimentoPastoSchedaTemplate savedApt = alimentoPastoSchedaTemplateRepo.save(newApt);
					savedTarget.getAlimenti().add(savedApt);

					// Clona le alternative
					for (AlimentoSchedaTemplateAlternativa srcAlt : srcApt.getAlternative()) {
						AlimentoSchedaTemplateAlternativa newAlt = new AlimentoSchedaTemplateAlternativa();
						newAlt.setAlimentoPastoSchedaTemplate(savedApt);
						newAlt.setAlimentoAlternativo(srcAlt.getAlimentoAlternativo());
						newAlt.setQuantita(srcAlt.getQuantita());
						newAlt.setPriorita(srcAlt.getPriorita());
						newAlt.setMode(srcAlt.getMode());
						newAlt.setManual(srcAlt.getManual());
						newAlt.setNote(srcAlt.getNote());
						newAlt.setNomeCustom(srcAlt.getNomeCustom());
						alternativaRepo.save(newAlt);
					}
				}
			}
		}

		return DtoMapper.toSchedaTemplateDto(repo.findByIdWithFullTree(templateId).orElseThrow());
	}

	// ═══════════════════════════════════════════
	// METODI PRIVATI
	// ═══════════════════════════════════════════

	private void checkOwnership(SchedaTemplate st, Long userId) {
		if (st.getCreatedBy() == null || !st.getCreatedBy().getId().equals(userId)) {
			throw new ForbiddenException("NON AUTORIZZATO: template scheda non accessibile");
		}
	}

	// ═══════════════════════════════════════════
	// PASTI DI DEFAULT — specchia ensureDefaultMeals() di SchedaService
	// ═══════════════════════════════════════════

	/**
	 * Inizializza i 4 pasti di default (Colazione, Pranzo, Merenda, Cena)
	 * all'interno del template, con gli orari configurati in DefaultMealTimesService.
	 * Per i template SETTIMANALI crea le stesse 4 voci × 7 giorni (28 righe),
	 * specchiando il comportamento di SchedaService.ensureDefaultMeals().
	 */
	private void ensureDefaultPastiTemplate(SchedaTemplate st) {
		record DefaultPasto(String nome, int ordine) {}
		List<DefaultPasto> defaults = List.of(
				new DefaultPasto("Colazione", 1),
				new DefaultPasto("Pranzo",    2),
				new DefaultPasto("Merenda",   3),
				new DefaultPasto("Cena",      4)
		);

		// Helper locale: costruisce un PastoSchedaTemplate già configurato
		if (TipoScheda.SETTIMANALE.equals(st.getTipo())) {
			for (GiornoSettimana giorno : GiornoSettimana.values()) {
				for (DefaultPasto dp : defaults) {
					st.getPasti().add(buildDefaultPasto(st, dp.nome(), dp.ordine(), giorno));
				}
			}
		} else {
			for (DefaultPasto dp : defaults) {
				st.getPasti().add(buildDefaultPasto(st, dp.nome(), dp.ordine(), null));
			}
		}
	}

	/**
	 * Costruisce un singolo PastoSchedaTemplate di default applicando gli orari
	 * tramite un adattatore temporaneo su Pasto (DefaultMealTimesService lavora su Pasto).
	 */
	private PastoSchedaTemplate buildDefaultPasto(SchedaTemplate st, String nome, int ordine, GiornoSettimana giorno) {
		// Usa Pasto come DTO temporaneo per sfruttare la logica orari esistente
		Pasto tmp = new Pasto();
		tmp.setNome(nome);
		defaultMealTimesService.applyDefaultTimesIfMissing(tmp);

		PastoSchedaTemplate p = new PastoSchedaTemplate();
		p.setSchedaTemplate(st);
		p.setNome(nome);
		p.setOrdineVisualizzazione(ordine);
		p.setGiorno(giorno);
		p.setOrarioInizio(tmp.getOrarioInizio());
		p.setOrarioFine(tmp.getOrarioFine());
		return p;
	}

	private void applyPasti(SchedaTemplate st, List<PastoSchedaTemplateUpsertDto> pastiDto) {
		st.getPasti().clear();
		if (pastiDto == null || pastiDto.isEmpty()) return;

		int ordine = 0;
		for (var pastoDto : pastiDto) {
			if (pastoDto == null) continue;

			PastoSchedaTemplate pasto = new PastoSchedaTemplate();
			pasto.setSchedaTemplate(st);
			pasto.setNome(pastoDto.getNome().trim());
			pasto.setDescrizione(normalizeString(pastoDto.getDescrizione()));
			pasto.setGiorno(parseGiorno(pastoDto.getGiorno()));
			pasto.setOrdineVisualizzazione(
					pastoDto.getOrdineVisualizzazione() != null ? pastoDto.getOrdineVisualizzazione() : ordine);
			pasto.setOrarioInizio(parseTime(pastoDto.getOrarioInizio()));
			pasto.setOrarioFine(parseTime(pastoDto.getOrarioFine()));
			applyAlimenti(pasto, pastoDto.getAlimentiPasto());

			st.getPasti().add(pasto);
			ordine++;
		}
	}

	/**
	 * FIX N+1: invece di N chiamate findById() in loop, carica tutti gli
	 * AlimentoBase richiesti in una sola query (findAllById → IN clause),
	 * poi risolve ogni DTO tramite Map in O(1).
	 */
	private void applyAlimenti(PastoSchedaTemplate pasto, List<AlimentoPastoSchedaTemplateUpsertDto> alimentiDto) {
		pasto.getAlimenti().clear();
		if (alimentiDto == null || alimentiDto.isEmpty()) return;

		// 1. Raccoglie tutti gli id richiesti (filtra null)
		List<Long> alimentoIds = alimentiDto.stream()
				.filter(dto -> dto != null && dto.getAlimentoId() != null)
				.map(AlimentoPastoSchedaTemplateUpsertDto::getAlimentoId)
				.distinct()
				.collect(Collectors.toList());

		if (alimentoIds.isEmpty()) return;

		// 2. UNA sola query IN clause → nessun N+1
		Map<Long, AlimentoBase> alimentoMap = alimentoBaseRepository.findAllById(alimentoIds).stream()
				.collect(Collectors.toMap(AlimentoBase::getId, Function.identity()));

		// 3. Assembla gli AlimentoPastoSchedaTemplate con lookup O(1)
		for (var alimentoDto : alimentiDto) {
			if (alimentoDto == null || alimentoDto.getAlimentoId() == null) continue;

			AlimentoBase alimento = alimentoMap.get(alimentoDto.getAlimentoId());
			if (alimento == null) {
				throw new NotFoundException("Alimento non trovato: " + alimentoDto.getAlimentoId());
			}

			AlimentoPastoSchedaTemplate apt = new AlimentoPastoSchedaTemplate();
			apt.setPastoSchedaTemplate(pasto);
			apt.setAlimento(alimento);
			apt.setQuantita(Math.max(1, alimentoDto.getQuantita()));
			apt.setNomeCustom(normalizeNomeCustom(alimentoDto.getNomeCustom(), alimento.getNome()));

			pasto.getAlimenti().add(apt);
		}
	}

	/**
	 * Clona i pasti del template nella scheda reale, incluse le alternative.
	 * Clona i pasti del template nella scheda reale.
	 * Segue il pattern PROVATO di SchedaService.copyDay():
	 * 1) Salva Pasto → genera ID
	 * 2) Salva AlimentoPasto → genera ID
	 * 3) Salva AlimentoAlternativo con riferimenti validi
	 *
	 * @param mergeMode se true, cerca pasti esistenti per nome+giorno e li riutilizza
	 */
	private void clonaPastiSuScheda(SchedaTemplate st, Scheda scheda, boolean mergeMode) {
		// 1. Bulk-load alternative dal template (zero N+1)
		List<Long> allAptIds = st.getPasti().stream()
				.flatMap(pt -> pt.getAlimenti().stream())
				.map(AlimentoPastoSchedaTemplate::getId)
				.collect(Collectors.toList());

		Map<Long, List<AlimentoSchedaTemplateAlternativa>> altByAptId = Map.of();
		if (!allAptIds.isEmpty()) {
			altByAptId = alternativaRepo.findAllByAptIdInWithAlimento(allAptIds).stream()
					.collect(Collectors.groupingBy(
							a -> a.getAlimentoPastoSchedaTemplate().getId()));
		}

		int maxOrdine = scheda.getPasti().stream()
				.mapToInt(p -> p.getOrdineVisualizzazione() != null ? p.getOrdineVisualizzazione() : 0)
				.max().orElse(-1);

		for (PastoSchedaTemplate pt : st.getPasti()) {
			Pasto pasto = null;

			// MERGE: cerca un pasto esistente con lo stesso nome + giorno
			if (mergeMode) {
				pasto = scheda.getPasti().stream()
						.filter(p -> p.getNome() != null && p.getNome().equals(pt.getNome())
								&& java.util.Objects.equals(p.getGiorno(), pt.getGiorno()))
						.findFirst()
						.orElse(null);
				if (pasto != null) {
					pasto.getAlimentiPasto().clear();
				}
			}

			if (pasto == null) {
				pasto = new Pasto();
				pasto.setScheda(scheda);
				pasto.setNome(pt.getNome());
				pasto.setDescrizione(pt.getDescrizione());
				pasto.setGiorno(pt.getGiorno());
				maxOrdine++;
				pasto.setOrdineVisualizzazione(maxOrdine);
				pasto.setEliminabile(true);
				pasto.setOrarioInizio(pt.getOrarioInizio());
				pasto.setOrarioFine(pt.getOrarioFine());
				scheda.getPasti().add(pasto);
			}

			// STEP 1: Salva il Pasto → genera ID
			Pasto savedPasto = pastoRepo.save(pasto);

			// STEP 2: Per ogni alimento, salva AlimentoPasto → genera ID
			for (AlimentoPastoSchedaTemplate apt : pt.getAlimenti()) {
				AlimentoPasto ap = new AlimentoPasto();
				ap.setPasto(savedPasto);
				ap.setAlimento(apt.getAlimento());
				ap.setQuantita(apt.getQuantita());
				AlimentoPasto savedAp = alimentoPastoRepo.save(ap);
				savedPasto.getAlimentiPasto().add(savedAp);

				// STEP 3: Clona le alternative con riferimento a savedAp (che ha ID)
				List<AlimentoSchedaTemplateAlternativa> alts =
						altByAptId.getOrDefault(apt.getId(), List.of());
				for (AlimentoSchedaTemplateAlternativa altT : alts) {
					AlimentoAlternativo altP = new AlimentoAlternativo();
					altP.setAlimentoPasto(savedAp);
					altP.setPasto(savedPasto);
					altP.setAlimentoAlternativo(altT.getAlimentoAlternativo());
					altP.setQuantita(altT.getQuantita());
					altP.setPriorita(altT.getPriorita());
					altP.setMode(altT.getMode());
					altP.setManual(altT.getManual());
					altP.setNote(altT.getNote());
					altP.setNomeCustom(altT.getNomeCustom());
					alimentoAlternativoRepo.save(altP);
				}
			}
		}
	}

	private String normalizeString(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeNomeCustom(String nomeCustom, String baseNome) {
		if (nomeCustom == null) return null;
		String trimmed = nomeCustom.trim();
		if (trimmed.isEmpty()) return null;
		if (baseNome != null && trimmed.equalsIgnoreCase(baseNome.trim())) return null;
		return trimmed;
	}

	private TipoScheda parseTipoScheda(String tipo) {
		if (tipo == null) return TipoScheda.GIORNALIERA;
		try {
			return TipoScheda.valueOf(tipo.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Tipo scheda non valido: " + tipo + ". Valori ammessi: GIORNALIERA, SETTIMANALE");
		}
	}

	private GiornoSettimana parseGiorno(String giorno) {
		if (giorno == null || giorno.isBlank()) return null;
		try {
			return GiornoSettimana.valueOf(giorno.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private LocalTime parseTime(String time) {
		if (time == null || time.isBlank()) return null;
		try {
			return LocalTime.parse(time);
		} catch (Exception e) {
			return null;
		}
	}
}
