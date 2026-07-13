package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.OrariStudioRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;

@Service
public class OwnershipValidator {

	@Autowired
	private CurrentUserService currentUserService;

	@Autowired
	private AuditService auditService;

	@Autowired
	private AlimentoPastoRepository alimentoPastoRepository;

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private SchedaRepository schedaRepository;

	@Autowired
	private PastoRepository pastoRepository;

	@Autowired
	private AppuntamentoRepository appuntamentoRepository;

	@Autowired
	private MisurazioneAntropometricaRepository misurazioneAntropometricaRepository;

	@Autowired
	private PlicometriaRepository plicometriaRepository;

	@Autowired
	private OrariStudioRepository orariStudioRepository;

	@Autowired
	private DocumentoFascicoloRepository documentoFascicoloRepository;

	@Autowired
	private CalcoloTdeeRepository calcoloTdeeRepository;

	/**
	 * Registra l'accesso NEGATO (audit A7) e costruisce la {@link ForbiddenException} da lanciare.
	 * L'audit non deve mai mascherare/impedire il 403 → try/catch di sicurezza. Coperte solo le
	 * risorse cliniche top-level (dove {@code entityId} combacia col tipo): {@code AlimentoPasto}/
	 * {@code Pasto} (sub-risorse di scheda) e {@code Appuntamento}/{@code OrariStudio} (non art. 9)
	 * non generano un evento DENIED dedicato.
	 */
	private ForbiddenException deny(AuditEntityType tipo, Long entityId, String messaggio) {
		try {
			auditService.recordDenied(tipo, entityId);
		} catch (Exception ignore) {
			// L'audit del diniego è best-effort: non deve impedire la risposta 403.
		}
		return new ForbiddenException(messaggio);
	}

	public AlimentoPasto getOwnedAlimentoPasto(Long alimentoPastoId) {
		var me = currentUserService.getMe();
		return alimentoPastoRepository.findByIdAndPasto_Scheda_Cliente_Nutrizionista_Id(alimentoPastoId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alimento pasto non accessibile"));
	}

	public Cliente getOwnedCliente(Long clienteId) {
		var me = currentUserService.getMe();
		return clienteRepository.findByIdAndNutrizionista_Id(clienteId, me.getId())
				.orElseThrow(() -> deny(AuditEntityType.CLIENTE, clienteId, "NON AUTORIZZATO: cliente non accessibile"));
	}

	public Scheda getOwnedScheda(Long schedaId) {
		var me = currentUserService.getMe();
		return schedaRepository.findByIdAndCliente_Nutrizionista_Id(schedaId, me.getId())
				.orElseThrow(() -> deny(AuditEntityType.SCHEDA, schedaId, "NON AUTORIZZATO: scheda non accessibile"));
	}

	public Scheda getOwnedSchedaWithPastiAndAlimenti(Long schedaId) {
		var me = currentUserService.getMe();
		return schedaRepository.findByIdWithPastiAndAlimentiMine(schedaId, me.getId())
				.orElseThrow(() -> deny(AuditEntityType.SCHEDA, schedaId, "NON AUTORIZZATO: scheda non accessibile"));
	}

	public Pasto getOwnedPasto(Long pastoId) {
		var me = currentUserService.getMe();
		return pastoRepository.findByIdAndScheda_Cliente_Nutrizionista_Id(pastoId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: pasto non accessibile"));
	}

	public Appuntamento getOwnedAppuntamento(Long appuntamentoId) {
		var me = currentUserService.getMe();
		return appuntamentoRepository.findByIdAndNutrizionista_Id(appuntamentoId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: appuntamento non accessibile"));
	}

	public MisurazioneAntropometrica getOwnedMisurazioneAntropometrica(Long id) {
		var me = currentUserService.getMe();
		return misurazioneAntropometricaRepository.findByIdAndCliente_Nutrizionista_Id(id, me.getId())
				.orElseThrow(() -> deny(AuditEntityType.MISURAZIONE_ANTROPOMETRICA, id, "NON AUTORIZZATO: misurazione non accessibile"));
	}

	public Plicometria getOwnedPlicometria(Long id) {
		var me = currentUserService.getMe();
		return plicometriaRepository.findByIdAndCliente_Nutrizionista_Id(id, me.getId())
				.orElseThrow(() -> deny(AuditEntityType.PLICOMETRIA, id, "NON AUTORIZZATO: plicometria non accessibile"));
	}
	// Aggiungi questo metodo nel tuo OwnershipValidator
	public Scheda getOwnedSchedaFullDetails(Long schedaId) {
	    var me = currentUserService.getMe();
	    return schedaRepository.findByIdWithFullDetailsMine(schedaId, me.getId())
	            .orElseThrow(() -> deny(AuditEntityType.SCHEDA, schedaId, "NON AUTORIZZATO: scheda non accessibile"));
	}

	public OrariStudio getOwnedOrariStudio(Long id) {
		var me = currentUserService.getMe();
		return orariStudioRepository.findByIdAndNutrizionista_Id(id, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: orario studio non accessibile"));
	}

	public DocumentoFascicolo getOwnedDocumentoFascicolo(Long id) {
		var me = currentUserService.getMe();
		return documentoFascicoloRepository.findByIdAndCliente_Nutrizionista_Id(id, me.getId())
				.orElseThrow(() -> deny(AuditEntityType.DOCUMENTO_FASCICOLO, id, "NON AUTORIZZATO: documento fascicolo non accessibile"));
	}

	public CalcoloTdee getOwnedCalcoloTdee(Long id) {
		var me = currentUserService.getMe();
		return calcoloTdeeRepository.findByIdAndCliente_Nutrizionista_Id(id, me.getId())
				.orElseThrow(() -> deny(AuditEntityType.CALCOLO_TDEE, id, "NON AUTORIZZATO: calcolo TDEE non accessibile"));
	}
}
