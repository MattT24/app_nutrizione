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
import it.nutrizionista.restnutrizionista.dto.ClienteDropdownDto;
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
    public ResponseEntity<AppuntamentoDto> update(@PathVariable("id") Long id, @Valid @RequestBody AppuntamentoFormDto form) {
        return ResponseEntity.ok(service.update(id, form));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public ResponseEntity<AppuntamentoDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public List<CalendarEventDto> myEvents(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return service.getMyCalendarEvents(start, end);
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public List<AppuntamentoDto> getUpcoming() {
        return service.getUpcoming();
    }

    // Drag & drop / resize
    @PatchMapping("/{id}/move")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_UPDATE')")
    public ResponseEntity<AppuntamentoDto> move(
        @PathVariable("id") Long id,
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(service.moveResize(id, start, end));
    }
    
    @GetMapping("/me/clienti/dropdown")
    @PreAuthorize("hasAuthority('APPUNTAMENTO_READ')")
    public List<ClienteDropdownDto> dropdownClienti(@RequestParam("q") String q) {
        return service.searchMyClientsForDropdown(q);
    }

}