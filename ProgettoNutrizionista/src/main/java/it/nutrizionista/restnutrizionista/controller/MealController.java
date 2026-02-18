package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.MealCreateRequest;
import it.nutrizionista.restnutrizionista.dto.MealUpdateRequest;
import it.nutrizionista.restnutrizionista.dto.PastoDto;
import it.nutrizionista.restnutrizionista.service.MealService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/meals")
public class MealController {
	@Autowired
	private MealService service;

	@PostMapping
	@PreAuthorize("hasAuthority('MEAL_CREATE')")
	public ResponseEntity<PastoDto> create(@Valid @RequestBody MealCreateRequest req) {
		return ResponseEntity.ok(service.create(req));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('MEAL_UPDATE')")
	public ResponseEntity<PastoDto> update(@PathVariable Long id, @Valid @RequestBody MealUpdateRequest req) {
		return ResponseEntity.ok(service.update(id, req));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('MEAL_DELETE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
