package it.nutrizionista.restnutrizionista.controller;

import it.nutrizionista.restnutrizionista.dto.PromemoriaDto;
import it.nutrizionista.restnutrizionista.dto.PromemoriaFormDto;
import it.nutrizionista.restnutrizionista.service.PromemoriaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/promemoria")
public class PromemoriaController {

    private final PromemoriaService service;

    public PromemoriaController(PromemoriaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PromemoriaDto> create(@Valid @RequestBody PromemoriaFormDto form) {
        return new ResponseEntity<>(service.create(form), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromemoriaDto> update(@PathVariable("id") Long id, @Valid @RequestBody PromemoriaFormDto form) {
        return ResponseEntity.ok(service.update(id, form));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromemoriaDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PromemoriaDto>> getByDateRange(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.getByDateRange(start, end));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<PromemoriaDto> move(
            @PathVariable("id") Long id,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(service.moveResize(id, start, end));
    }
}
