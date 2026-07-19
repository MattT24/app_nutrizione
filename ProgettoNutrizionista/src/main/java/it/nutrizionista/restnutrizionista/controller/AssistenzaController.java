package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.*;
import it.nutrizionista.restnutrizionista.service.AssistenzaService;
import jakarta.validation.Valid;

/** API di assistenza del nutrizionista, protette dal permesso incluso nel JWT. */
@RestController
@RequestMapping("/api/assistenza")
@PreAuthorize("hasAuthority('ASSISTENZA_USE')")
public class AssistenzaController {
    @Autowired private AssistenzaService assistenzaService;

    @PostMapping("/tickets")
    public ResponseEntity<TicketAssistenzaDto> crea(@Valid @RequestBody CreaTicketAssistenzaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assistenzaService.creaTicket(request));
    }
    @GetMapping("/tickets/attivo")
    public ResponseEntity<TicketAssistenzaDto> attivo() {
        TicketAssistenzaDto ticket = assistenzaService.ticketAttivo();
        return ticket == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(ticket);
    }
    /** Endpoint di dettaglio, incluso per testare esplicitamente la protezione anti-IDOR. */
    @GetMapping("/tickets/{id}")
    public TicketAssistenzaDto dettaglio(@PathVariable Long id) { return assistenzaService.ticketMio(id); }
    @GetMapping("/tickets")
    public PageResponse<TicketAssistenzaDto> storico(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return assistenzaService.storico(page, size); }
    @PostMapping("/tickets/{id}/messaggi")
    public ResponseEntity<MessaggioAssistenzaDto> invia(@PathVariable Long id,
            @Valid @RequestBody CreaMessaggioAssistenzaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assistenzaService.inviaMessaggioMio(id, request));
    }
    @GetMapping("/tickets/{id}/messaggi")
    public PollingMessaggiAssistenzaDto messaggi(@PathVariable Long id,
            @RequestParam(defaultValue = "0") Long afterId) { return assistenzaService.messaggiMiei(id, afterId); }
    @PostMapping("/tickets/{id}/chiudi")
    public TicketAssistenzaDto chiudi(@PathVariable Long id) { return assistenzaService.chiudiMio(id); }
    @PostMapping("/tickets/{id}/messaggi/letti")
    public ResponseEntity<Void> letti(@PathVariable Long id) {
        assistenzaService.marcaLettiMio(id);
        return ResponseEntity.noContent().build();
    }
}
