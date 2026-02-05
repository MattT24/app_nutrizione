package it.nutrizionista.restnutrizionista.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.AppuntamentoDto;
import it.nutrizionista.restnutrizionista.dto.AppuntamentoFormDto;
import it.nutrizionista.restnutrizionista.dto.CalendarEventDto;
import it.nutrizionista.restnutrizionista.service.AppuntamentoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appuntamenti")
@CrossOrigin(origins = "http://localhost:4200")
public class AppuntamentoController {

    private final AppuntamentoService service;

    public AppuntamentoController(AppuntamentoService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('APPUNTAMENTO_CREATE')")
    public ResponseEntity<AppuntamentoDto> create(@Valid @RequestBody AppuntamentoFormDto form) {
        return ResponseEntity.status(201).body(service.create(form));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_UPDATE')")
    public ResponseEntity<AppuntamentoDto> update(@PathVariable Long id, @Valid @RequestBody AppuntamentoFormDto form) {
        return ResponseEntity.ok(service.update(id, form));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public ResponseEntity<AppuntamentoDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // FullCalendar range
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public List<CalendarEventDto> myEvents(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return service.getMyCalendarEvents(start, end);
    }

    // Drag & drop / resize
    @PatchMapping("/{id}/move")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_UPDATE')")
    public ResponseEntity<AppuntamentoDto> move(
        @PathVariable Long id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(service.moveResize(id, start, end));
    }
}

