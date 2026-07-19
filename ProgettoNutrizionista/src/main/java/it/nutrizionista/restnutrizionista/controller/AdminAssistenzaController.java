package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.*;
import it.nutrizionista.restnutrizionista.enums.StatoTicket;
import it.nutrizionista.restnutrizionista.service.AssistenzaService;
import jakarta.validation.Valid;

/** API assistenza riservate al super admin, anche dalla filter chain su /api/admin/**. */
@RestController
@RequestMapping("/api/admin/assistenza")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminAssistenzaController {
    @Autowired private AssistenzaService assistenzaService;

    @GetMapping("/tickets")
    public PageResponse<TicketAssistenzaDto> lista(@RequestParam StatoTicket stato,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return assistenzaService.listaAdmin(stato, page, size);
    }
    @GetMapping("/tickets/conteggi")
    public ConteggiAssistenzaDto conteggi() { return assistenzaService.conteggi(); }
    @PostMapping("/tickets/{id}/accetta")
    public TicketAssistenzaDto accetta(@PathVariable Long id) { return assistenzaService.accetta(id); }
    @PostMapping("/tickets/{id}/rifiuta")
    public TicketAssistenzaDto rifiuta(@PathVariable Long id,
            @Valid @RequestBody(required = false) RifiutaTicketAssistenzaRequest request) {
        return assistenzaService.rifiuta(id, request == null ? null : request.getMotivo());
    }
    @PostMapping("/tickets/{id}/chiudi")
    public TicketAssistenzaDto chiudi(@PathVariable Long id) { return assistenzaService.chiudiAdmin(id); }
    @PostMapping("/tickets/{id}/messaggi")
    public ResponseEntity<MessaggioAssistenzaDto> invia(@PathVariable Long id,
            @Valid @RequestBody CreaMessaggioAssistenzaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assistenzaService.inviaMessaggioAdmin(id, request));
    }
    @GetMapping("/tickets/{id}/messaggi")
    public PollingMessaggiAssistenzaDto messaggi(@PathVariable Long id,
            @RequestParam(defaultValue = "0") Long afterId) { return assistenzaService.messaggiAdmin(id, afterId); }
    @PostMapping("/tickets/{id}/messaggi/letti")
    public ResponseEntity<Void> letti(@PathVariable Long id) {
        assistenzaService.marcaLettiAdmin(id);
        return ResponseEntity.noContent().build();
    }

    /* Upgrade futuro: spring-boot-starter-websocket + STOMP con JWT nell'handshake.
       Non implementato: il polling HTTP riusa intenzionalmente la sicurezza stateless esistente. */
}
