package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ObiettivoNutrizionaleDto;
import it.nutrizionista.restnutrizionista.dto.ObiettivoNutrizionaleFormDto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.ObiettivoNutrizionale;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoObiettivo;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ObiettivoNutrizionaleRepository;
import jakarta.validation.Valid;

@Service
public class ObiettivoNutrizionaleService {

	@Autowired
	private ObiettivoNutrizionaleRepository repo;
	@Autowired
	private OwnershipValidator ownershipValidator;

	/**
	 * Restituisce l'obiettivo nutrizionale ATTIVO del cliente (se esiste).
	 */
	@Transactional(readOnly = true)
	public ObiettivoNutrizionaleDto getByClienteId(Long clienteId) {
		ownershipValidator.getOwnedCliente(clienteId);
		return repo.findByCliente_IdAndAttivoTrue(clienteId)
				.map(DtoMapper::toObiettivoNutrizionaleDto)
				.orElse(null);
	}

	/**
	 * Restituisce lo storico completo degli obiettivi del cliente,
	 * ordinati per data creazione decrescente.
	 */
	@Transactional(readOnly = true)
	public List<ObiettivoNutrizionaleDto> getStoricoByClienteId(Long clienteId) {
		ownershipValidator.getOwnedCliente(clienteId);
		return repo.findByCliente_IdOrderByDataCreazioneDesc(clienteId)
				.stream()
				.map(DtoMapper::toObiettivoNutrizionaleDto)
				.toList();
	}

	/**
	 * Crea o aggiorna l'obiettivo nutrizionale ATTIVO per un cliente.
	 * Se non esiste un obiettivo attivo, ne crea uno nuovo.
	 */
	@Transactional
	public ObiettivoNutrizionaleDto creaOAggiorna(Long clienteId, @Valid ObiettivoNutrizionaleFormDto form) {
		Cliente cliente = ownershipValidator.getOwnedCliente(clienteId);

		ObiettivoNutrizionale ob = repo.findByCliente_IdAndAttivoTrue(clienteId)
				.orElseGet(() -> {
					ObiettivoNutrizionale nuovo = new ObiettivoNutrizionale();
					nuovo.setCliente(cliente);
					nuovo.setAttivo(true);
					nuovo.setDataCreazione(LocalDate.now());
					return nuovo;
				});

		ob.setObiettivo(form.getObiettivo());
		ob.setNote(form.getNote());

		// Copia i macro target dal form
		ob.setTargetCalorie(form.getTargetCalorie());
		ob.setTargetProteine(form.getTargetProteine());
		ob.setTargetCarboidrati(form.getTargetCarboidrati());
		ob.setTargetGrassi(form.getTargetGrassi());
		ob.setTargetFibre(form.getTargetFibre());
		ob.setPctProteine(form.getPctProteine());
		ob.setPctCarboidrati(form.getPctCarboidrati());
		ob.setPctGrassi(form.getPctGrassi());

		// Lock states
		ob.setLockedPctProteine(form.getLockedPctProteine() != null ? form.getLockedPctProteine() : false);
		ob.setLockedPctCarboidrati(form.getLockedPctCarboidrati() != null ? form.getLockedPctCarboidrati() : false);
		ob.setLockedPctGrassi(form.getLockedPctGrassi() != null ? form.getLockedPctGrassi() : false);
		ob.setLockedGProteine(form.getLockedGProteine() != null ? form.getLockedGProteine() : false);
		ob.setLockedGCarboidrati(form.getLockedGCarboidrati() != null ? form.getLockedGCarboidrati() : false);
		ob.setLockedGGrassi(form.getLockedGGrassi() != null ? form.getLockedGGrassi() : false);

		// Tentativo di calcolo BMR/TDEE (best-effort, non blocca se dati mancanti)
		try {
			calcolaBmrTdee(ob, cliente);
		} catch (Exception e) {
			// BMR/TDEE rimangono null se dati insufficienti
		}

		return DtoMapper.toObiettivoNutrizionaleDto(repo.save(ob));
	}

	/**
	 * Attiva un obiettivo specifico, disattivando quello corrente.
	 */
	@Transactional
	public ObiettivoNutrizionaleDto attivaObiettivo(Long clienteId, Long obiettivoId) {
		ownershipValidator.getOwnedCliente(clienteId);

		// Disattiva l'obiettivo corrente (se esiste)
		repo.findByCliente_IdAndAttivoTrue(clienteId).ifPresent(corrente -> {
			corrente.setAttivo(false);
			repo.save(corrente);
		});

		// Attiva quello selezionato
		ObiettivoNutrizionale target = repo.findById(obiettivoId)
				.filter(ob -> ob.getCliente().getId().equals(clienteId))
				.orElseThrow(() -> new RuntimeException("Obiettivo non trovato"));

		target.setAttivo(true);
		return DtoMapper.toObiettivoNutrizionaleDto(repo.save(target));
	}

	/**
	 * Ricalcola BMR e TDEE dai dati aggiornati del cliente.
	 * Restituisce i valori calcolati SENZA salvarli nel database.
	 * Il salvataggio avviene solo tramite creaOAggiorna().
	 * 
	 * @return lista campi mancanti (vuota se calcolo riuscito)
	 */
	@Transactional(readOnly = true)
	public CalcoloResult calcola(Long clienteId) {
		Cliente cliente = ownershipValidator.getOwnedCliente(clienteId);

		// Verifica campi necessari
		List<String> campiMancanti = verificaCampiCalcolo(cliente);
		if (!campiMancanti.isEmpty()) {
			return new CalcoloResult(null, campiMancanti);
		}

		ObiettivoNutrizionale ob = repo.findByCliente_IdAndAttivoTrue(clienteId)
				.orElseGet(() -> {
					ObiettivoNutrizionale nuovo = new ObiettivoNutrizionale();
					nuovo.setCliente(cliente);
					nuovo.setObiettivo(TipoObiettivo.MANTENIMENTO);
					nuovo.setAttivo(true);
					nuovo.setDataCreazione(LocalDate.now());
					return nuovo;
				});

		calcolaBmrTdee(ob, cliente);

		// Se non ci sono target manuali, calcola da TDEE + obiettivo
		TipoObiettivo tipo = ob.getObiettivo();
		double kcalTarget = ob.getTdee() * tipo.getMoltiplicatoreTdee();
		ob.setTargetCalorie(Math.round(kcalTarget * 10.0) / 10.0);

		// Se le percentuali non sono state impostate manualmente, usa i default
		if (ob.getPctProteine() == null) {
			ob.setPctProteine((double) tipo.getPctProteine());
		}
		if (ob.getPctCarboidrati() == null) {
			ob.setPctCarboidrati((double) tipo.getPctCarboidrati());
		}
		if (ob.getPctGrassi() == null) {
			ob.setPctGrassi((double) tipo.getPctGrassi());
		}

		// Calcola grammi da percentuali
		ob.setTargetProteine(Math.round(ob.getTargetCalorie() * ob.getPctProteine() / 100.0 / 4.0 * 10.0) / 10.0);
		ob.setTargetCarboidrati(
				Math.round(ob.getTargetCalorie() * ob.getPctCarboidrati() / 100.0 / 4.0 * 10.0) / 10.0);
		ob.setTargetGrassi(Math.round(ob.getTargetCalorie() * ob.getPctGrassi() / 100.0 / 9.0 * 10.0) / 10.0);

		if (ob.getTargetFibre() == null) {
			ob.setTargetFibre(25.0); // default OMS
		}

		// Restituisce anteprima SENZA salvare — il salvataggio avviene solo con "Salva Obiettivo"
		ObiettivoNutrizionaleDto dto = DtoMapper.toObiettivoNutrizionaleDto(ob);
		return new CalcoloResult(dto, List.of());
	}

	/**
	 * Elimina un obiettivo nutrizionale specifico.
	 */
	@Transactional
	public void delete(Long clienteId, Long obiettivoId) {
		ownershipValidator.getOwnedCliente(clienteId);
		repo.deleteByIdAndCliente_Id(obiettivoId, clienteId);
	}

	// ─── Calcolo Mifflin-St Jeor ─────────────────────────────────────────

	private void calcolaBmrTdee(ObiettivoNutrizionale ob, Cliente cliente) {
		int eta = Period.between(cliente.getDataNascita(), LocalDate.now()).getYears();
		double peso = cliente.getPeso();
		int altezza = cliente.getAltezza();

		double bmr;
		if (cliente.getSesso() == Sesso.Maschio) {
			bmr = 10 * peso + 6.25 * altezza - 5 * eta + 5;
		} else {
			bmr = 10 * peso + 6.25 * altezza - 5 * eta - 161;
		}

		bmr = Math.round(bmr * 10.0) / 10.0;
		ob.setBmr(bmr);

		double laf = (cliente.getLivelloDiAttivita() != null)
				? cliente.getLivelloDiAttivita().getLaf()
				: LivelloDiAttivita.MODERATAMENTE_ATTIVO.getLaf(); // default

		ob.setLaf(laf);
		ob.setTdee(Math.round(bmr * laf * 10.0) / 10.0);
	}

	private List<String> verificaCampiCalcolo(Cliente c) {
		List<String> mancanti = new ArrayList<>();
		if (c.getPeso() <= 0)
			mancanti.add("peso");
		if (c.getAltezza() <= 0)
			mancanti.add("altezza");
		if (c.getSesso() == null)
			mancanti.add("sesso");
		if (c.getDataNascita() == null)
			mancanti.add("dataNascita");
		if (c.getLivelloDiAttivita() == null)
			mancanti.add("livelloDiAttivita");
		return mancanti;
	}

	// ─── Result wrapper per il calcolo ───────────────────────────────────

	public record CalcoloResult(ObiettivoNutrizionaleDto obiettivo, List<String> campiMancanti) {
		public boolean isSuccesso() {
			return campiMancanti == null || campiMancanti.isEmpty();
		}
	}
}
