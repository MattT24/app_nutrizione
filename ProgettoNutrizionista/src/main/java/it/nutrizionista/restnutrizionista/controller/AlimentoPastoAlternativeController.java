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

import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoAlternativoUpsertDto;
import it.nutrizionista.restnutrizionista.service.AlimentoAlternativoService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_pasto/{alimentoPastoId}/alternative")
public class AlimentoPastoAlternativeController {

	@Autowired
	private AlimentoAlternativoService alimentoAlternativoService;

	@GetMapping
	@PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_READ')")
	public ResponseEntity<List<AlimentoAlternativoDto>> list(@PathVariable Long alimentoPastoId) {
		return ResponseEntity.ok(alimentoAlternativoService.listByAlimentoPasto(alimentoPastoId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_CREATE')")
	public ResponseEntity<AlimentoAlternativoDto> create(
			@PathVariable Long alimentoPastoId,
			@Valid @RequestBody AlimentoAlternativoUpsertDto body) {
		return ResponseEntity.status(201).body(alimentoAlternativoService.createForAlimentoPasto(alimentoPastoId, body));
	}

	@PutMapping("/{alternativeId}")
	@PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_UPDATE')")
	public ResponseEntity<AlimentoAlternativoDto> update(
			@PathVariable Long alimentoPastoId,
			@PathVariable Long alternativeId,
			@Valid @RequestBody AlimentoAlternativoUpsertDto body) {
		return ResponseEntity.ok(alimentoAlternativoService.updateForAlimentoPasto(alimentoPastoId, alternativeId, body));
	}

	@DeleteMapping("/{alternativeId}")
	@PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_DELETE')")
	public ResponseEntity<Void> delete(@PathVariable Long alimentoPastoId, @PathVariable Long alternativeId) {
		alimentoAlternativoService.deleteForAlimentoPasto(alimentoPastoId, alternativeId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping
	@PreAuthorize("hasAuthority('ALIMENTO_ALTERNATIVO_UPDATE')")
	public ResponseEntity<List<AlimentoAlternativoDto>> bulkUpsert(
			@PathVariable Long alimentoPastoId,
			@Valid @RequestBody List<AlimentoAlternativoUpsertDto> items) {
		return ResponseEntity.ok(alimentoAlternativoService.bulkUpsertForAlimentoPasto(alimentoPastoId, items));
	}
}

