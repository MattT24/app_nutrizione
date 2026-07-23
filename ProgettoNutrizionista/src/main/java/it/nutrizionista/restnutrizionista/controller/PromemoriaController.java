package it.nutrizionista.restnutrizionista.controller;

import it.nutrizionista.restnutrizionista.dto.PromemoriaDto;
import it.nutrizionista.restnutrizionista.dto.PromemoriaFormDto;
import it.nutrizionista.restnutrizionista.service.PromemoriaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API REST per i promemoria: calendario PERSONALE del nutrizionista (ownership enforced nel service via
 * {@code findByIdAndNutrizionista_Id}; NON cliente-scoped, NON dati art. 9). B3: ogni endpoint richiede un
 * permesso esplicito. Si riusa la famiglia {@code APPUNTAMENTO_*} (stesso dominio "calendario", già sul ruolo
 * NUTRIZIONISTA) invece di introdurre nuovi {@code PROMEMORIA_*} — nessuna migrazione/seed necessaria.
 */
@RestController
@RequestMapping("/api/promemoria")
public class PromemoriaController {

    private final PromemoriaService service;

    public PromemoriaController(PromemoriaService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('APPUNTAMENTO_CREATE')")
    public ResponseEntity<PromemoriaDto> create(@Valid @RequestBody PromemoriaFormDto form) {
        return new ResponseEntity<>(service.create(form), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_UPDATE')")
    public ResponseEntity<PromemoriaDto> update(@PathVariable("id") Long id, @Valid @RequestBody PromemoriaFormDto form) {
        return ResponseEntity.ok(service.update(id, form));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public ResponseEntity<PromemoriaDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public ResponseEntity<List<PromemoriaDto>> getByDateRange(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.getByDateRange(start, end));
    }

    @PatchMapping("/{id}/move")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_UPDATE')")
    public ResponseEntity<PromemoriaDto> move(
            @PathVariable("id") Long id,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(service.moveResize(id, start, end));
    }
}
