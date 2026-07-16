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
import org.springframework.util.StringUtils;

import it.nutrizionista.restnutrizionista.dto.ClienteDto;
import it.nutrizionista.restnutrizionista.dto.ClienteFormDto;
import it.nutrizionista.restnutrizionista.dto.ClienteInfoDto;
import it.nutrizionista.restnutrizionista.dto.ClienteLightDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PesoAltezzaRequest;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;
import it.nutrizionista.restnutrizionista.exception.BadRequestException;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoNomeOverrideRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AttivitaRecenteRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.EventoGamificationRepository;
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
	@Autowired private AttivitaRecenteRepository attivitaRecenteRepository;
	@Autowired private EventoGamificationRepository eventoGamificationRepository;
	@Autowired private GamificationService gamificationService;
	@Autowired private FascicoloService fascicoloService;
	@Autowired private AuditService auditService;

	@Transactional
	public ClienteDto create(@Valid ClienteFormDto form) {
		Utente u = currentUserService.getMe();
		// Controllo duplicati su vincoli univoci, con messaggi chiari (409 Conflict).
		// CF/email sono FACOLTATIVI: si controlla il duplicato SOLO se valorizzati (più clienti
		// senza CF/email non collidono; l'unique DB ammette più NULL).
		if (StringUtils.hasText(form.getCodiceFiscale()) && repo.existsByCodiceFiscale(form.getCodiceFiscale())) {
			throw new ConflictException("Esiste già un cliente con questo codice fiscale");
		}
		if (StringUtils.hasText(form.getEmail()) && repo.existsByEmail(form.getEmail())) {
			throw new ConflictException("Esiste già un cliente con questa email");
		}
		Cliente c = DtoMapper.toCliente(form);
		c.setNutrizionista(u);
		Cliente salvato = repo.save(c);
		gamificationService.registraEvento(u, TipoEventoGamification.NUOVO_CLIENTE, salvato.getId());
		return DtoMapper.toClienteDtoLight(salvato);
	}


	@Transactional
	public ClienteDto update(@Valid ClienteFormDto form) {
		if (form.getId() == null) throw new BadRequestException("Id cliente obbligatorio per update");
		Cliente c = ownershipValidator.getOwnedCliente(form.getId());
		// Controllo duplicati escludendo il cliente stesso, solo se CF/email valorizzati (facoltativi).
		if (StringUtils.hasText(form.getCodiceFiscale()) && repo.existsByCodiceFiscaleAndIdNot(form.getCodiceFiscale(), form.getId())) {
			throw new ConflictException("Esiste già un cliente con questo codice fiscale");
		}
		if (StringUtils.hasText(form.getEmail()) && repo.existsByEmailAndIdNot(form.getEmail(), form.getId())) {
			throw new ConflictException("Esiste già un cliente con questa email");
		}
		DtoMapper.updateClienteFromForm(c, form);
		return DtoMapper.toClienteDto(repo.save(c));
	}

	@Transactional
	public ClienteDto updatePesoAltezza(@Valid PesoAltezzaRequest req) {
		if (req.getId() == null) throw new BadRequestException("Id cliente obbligatorio per update");
		Cliente c = ownershipValidator.getOwnedCliente(req.getId());
		if (req.getPeso() != null) c.setPeso(req.getPeso());
		if (req.getAltezza() != null) c.setAltezza(req.getAltezza());
		// Permettiamo di impostare anche a null il pesoTarget se inviato, se serve
		c.setPesoTarget(req.getPesoTarget());
		return DtoMapper.toClienteDto(repo.save(c));
	}

	/**
	 * Cancellazione completa del cliente (art. 17 GDPR: nessun dato sanitario orfano). Meccanismo
	 * <b>ORM-first ibrido</b>, tutto in un'unica transazione:
	 * <ol>
	 *   <li>{@link #svuotaAlberoSchede(Long)} — svuota il sotto-albero profondo delle schede;</li>
	 *   <li>{@link #eliminaFigliNonCascade(Long)} — i figli con FK verso {@code clienti} NON mappata
	 *       come collezione di {@code Cliente} (che il cascade ORM non tocca) + i file su disco +
	 *       l'anonimizzazione dei riferimenti denormalizzati;</li>
	 *   <li>{@code repo.delete(c)} — il cascade ORM rimuove le sole collezioni mono-FK di
	 *       {@code Cliente} (schede ormai vuote, misurazioni, plicometrie, obiettivi, blacklist, tag).</li>
	 * </ol>
	 * <b>NON</b> si usa {@code ON DELETE CASCADE} a livello DB: H2 (test) lo applica sempre, TiDB no
	 * fino a v8.5 → un orfano su TiDB resterebbe mascherato dai test verdi su H2. L'SQL emesso da
	 * ORM/bulk-delete è invece identico sui due DB. La completezza è verificata da
	 * {@code ClienteDeleteCascadeCompletoIntegrationTest} (regola di coverage in CLAUDE.md).
	 */
	@Transactional
	public void deleteMyCliente(Long id) {
	    if (id == null) throw new BadRequestException("Id cliente obbligatorio per il delete");

	    // Verifica ownership (carica solo il cliente, non l'albero delle schede)
	    Cliente c = ownershipValidator.getOwnedCliente(id);

	    // Audit critico (A7): DELETE nella STESSA transazione → la riga committa iff il delete committa.
	    auditService.recordCriticalSameTx(AuditAction.DELETE, AuditEntityType.CLIENTE, id, id);

	    svuotaAlberoSchede(id);
	    eliminaFigliNonCascade(id);

	    // Infine il cliente: il cascade ORM gestisce ora solo le collezioni mono-FK
	    // (schede ormai vuote, misurazioni, plicometrie, obiettivi, blacklist, tag).
	    repo.delete(c);
	}

	/**
	 * Svuota in modo robusto l'albero profondo di OGNI scheda del cliente, bottom-up. Non ci si può
	 * affidare al solo cascade ORM: {@code AlimentoAlternativo} ha due FK (alimento_pasto_id +
	 * pasto_id) ma solo {@code alimento_pasto_id} è coperta da {@code orphanRemoval}; con il batching
	 * JDBC questo genera {@code StaleStateException} ("row count 0; expected 1") sulla delete delle
	 * alternative. Stesso ordine bottom-up di {@code SchedaService.delete()}.
	 */
	private void svuotaAlberoSchede(Long id) {
	    for (Long schedaId : schedaRepository.findIdsByCliente_Id(id)) {
	        alimentoAlternativoRepository.bulkDeleteBySchedaId(schedaId);        // alternative (dipendono da alimenti_pasto E pasti)
	        alimentoPastoNomeOverrideRepository.bulkDeleteBySchedaId(schedaId);  // nome_override
	        alimentoPastoRepository.bulkDeleteBySchedaId(schedaId);              // alimenti_pasto
	        pastoRepository.bulkDeleteBySchedaId(schedaId);                      // pasti
	    }
	}

	/**
	 * Punto UNICO per i figli del cliente che il cascade ORM NON gestisce. Contratto di coverage
	 * (vedi CLAUDE.md): ogni nuova entità con FK verso {@code clienti} che NON sia una collezione
	 * {@code orphanRemoval} di {@code Cliente} va aggiunta QUI (+ seminata nel test seed-completo).
	 * <p>Coperto ALTROVE (cascade ORM su {@code Cliente}, non qui): schede→albero, misurazioni,
	 * plicometrie, obiettivi, blacklist (AvversionePersonale), tag ({@code @ElementCollection}).
	 * <p>Gestito QUI perché la FK {@code cliente_id} NON è una collezione cascade di {@code Cliente}:
	 * <ul>
	 *   <li>{@code Appuntamento} — delete esplicito;</li>
	 *   <li>{@code AttivitaRecente} — delete esplicito (senza, un cliente con storico non è
	 *       cancellabile: DataIntegrityViolation → 400);</li>
	 *   <li>{@code CalcoloTdee} — delete esplicito;</li>
	 *   <li>{@code DocumentoFascicolo} — record + file PDF su disco (A5.1); l'ORM non rimuove mai i file.</li>
	 * </ul>
	 * <p>Riferimenti denormalizzati SENZA FK (trattati all'OPPOSTO, non cancellati):
	 * <ul>
	 *   <li>{@code EventoGamification.clienteId} → <b>anonimizzato a NULL</b> (recide il legame col
	 *       paziente cancellato preservando lo storico del nutrizionista, art. 17);</li>
	 *   <li>{@code AuditLog.clienteId} → <b>PRESERVATO INTATTO</b> (retention A7, obbligo legale
	 *       art. 17(3)(b)): NON compare qui di proposito.</li>
	 * </ul>
	 */
	private void eliminaFigliNonCascade(Long id) {
	    appuntamentoRepository.deleteByCliente_Id(id);
	    attivitaRecenteRepository.deleteByCliente_Id(id);
	    calcoloTdeeRepository.deleteByClienteId(id);
	    fascicoloService.eliminaDocumentiDiCliente(id);      // record + file su disco
	    eventoGamificationRepository.anonimizzaClienteId(id); // clienteId → NULL (NON delete)
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
        // Audit A7 (async): la vista clinica completa del paziente è l'accesso in lettura più forte.
        auditService.record(AuditAction.READ, AuditEntityType.CLIENTE, id, id);
        return DtoMapper.toClienteInfoDto(c);
    }
	//manca cliente Fabbisogno, da studiare un attimo
}
