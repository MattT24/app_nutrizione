package it.nutrizionista.restnutrizionista.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.ObiettivoNutrizionaleDto;
import it.nutrizionista.restnutrizionista.dto.ObiettivoNutrizionaleFormDto;
import it.nutrizionista.restnutrizionista.service.ObiettivoNutrizionaleService;
import it.nutrizionista.restnutrizionista.service.ObiettivoNutrizionaleService.CalcoloResult;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/clienti/{clienteId}/obiettivo")
public class ObiettivoNutrizionaleController {

	@Autowired
	private ObiettivoNutrizionaleService service;

	/**
	 * GET /api/clienti/{clienteId}/obiettivo
	 * Restituisce l'obiettivo nutrizionale ATTIVO del cliente (o 204 se non esiste).
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('CLIENTE_DETTAGLIO')")
	public ResponseEntity<ObiettivoNutrizionaleDto> get(@PathVariable Long clienteId) {
		ObiettivoNutrizionaleDto dto = service.getByClienteId(clienteId);
		if (dto == null) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(dto);
	}

	/**
	 * GET /api/clienti/{clienteId}/obiettivo/storico
	 * Restituisce lo storico completo degli obiettivi del cliente.
	 */
	@GetMapping("/storico")
	@PreAuthorize("hasAuthority('CLIENTE_DETTAGLIO')")
	public ResponseEntity<List<ObiettivoNutrizionaleDto>> getStorico(@PathVariable Long clienteId) {
		List<ObiettivoNutrizionaleDto> lista = service.getStoricoByClienteId(clienteId);
		return ResponseEntity.ok(lista);
	}

	/**
	 * POST /api/clienti/{clienteId}/obiettivo
	 * Crea o aggiorna l'obiettivo nutrizionale attivo.
	 */
	@PostMapping
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<ObiettivoNutrizionaleDto> creaOAggiorna(
			@PathVariable Long clienteId,
			@Valid @RequestBody ObiettivoNutrizionaleFormDto form) {
		ObiettivoNutrizionaleDto saved = service.creaOAggiorna(clienteId, form);
		return ResponseEntity.ok(saved);
	}

	/**
	 * POST /api/clienti/{clienteId}/obiettivo/calcola
	 * Ricalcola BMR/TDEE e target macro dai dati del cliente.
	 * Se mancano campi, restituisce 422 con la lista dei campi mancanti.
	 */
	@PostMapping("/calcola")
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<?> calcola(@PathVariable Long clienteId) {
		CalcoloResult result = service.calcola(clienteId);
		if (!result.isSuccesso()) {
			return ResponseEntity.unprocessableEntity().body(
					java.util.Map.of("campiMancanti", result.campiMancanti()));
		}
		return ResponseEntity.ok(result.obiettivo());
	}

	/**
	 * PUT /api/clienti/{clienteId}/obiettivo/{obiettivoId}/attiva
	 * Attiva un obiettivo specifico, disattivando quello corrente.
	 */
	@PutMapping("/{obiettivoId}/attiva")
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<ObiettivoNutrizionaleDto> attiva(
			@PathVariable Long clienteId,
			@PathVariable Long obiettivoId) {
		ObiettivoNutrizionaleDto dto = service.attivaObiettivo(clienteId, obiettivoId);
		return ResponseEntity.ok(dto);
	}

	/**
	 * DELETE /api/clienti/{clienteId}/obiettivo/{obiettivoId}
	 * Elimina un obiettivo nutrizionale specifico.
	 */
	@DeleteMapping("/{obiettivoId}")
	@PreAuthorize("hasAuthority('CLIENTE_UPDATE')")
	public ResponseEntity<Void> delete(
			@PathVariable Long clienteId,
			@PathVariable Long obiettivoId) {
		service.delete(clienteId, obiettivoId);
		return ResponseEntity.noContent().build();
	}
}
