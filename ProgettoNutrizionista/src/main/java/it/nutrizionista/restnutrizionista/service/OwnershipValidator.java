package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;

@Service
public class OwnershipValidator {

	@Autowired
	private CurrentUserService currentUserService;

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

	public AlimentoPasto getOwnedAlimentoPasto(Long alimentoPastoId) {
		var me = currentUserService.getMe();
		return alimentoPastoRepository.findByIdAndPasto_Scheda_Cliente_Nutrizionista_Id(alimentoPastoId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: alimento pasto non accessibile"));
	}

	public Cliente getOwnedCliente(Long clienteId) {
		var me = currentUserService.getMe();
		return clienteRepository.findByIdAndNutrizionista_Id(clienteId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: cliente non accessibile"));
	}

	public Scheda getOwnedScheda(Long schedaId) {
		var me = currentUserService.getMe();
		return schedaRepository.findByIdAndCliente_Nutrizionista_Id(schedaId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: scheda non accessibile"));
	}

	public Scheda getOwnedSchedaWithPastiAndAlimenti(Long schedaId) {
		var me = currentUserService.getMe();
		return schedaRepository.findByIdWithPastiAndAlimentiMine(schedaId, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: scheda non accessibile"));
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
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: misurazione non accessibile"));
	}

	public Plicometria getOwnedPlicometria(Long id) {
		var me = currentUserService.getMe();
		return plicometriaRepository.findByIdAndCliente_Nutrizionista_Id(id, me.getId())
				.orElseThrow(() -> new ForbiddenException("NON AUTORIZZATO: plicometria non accessibile"));
	}
}
