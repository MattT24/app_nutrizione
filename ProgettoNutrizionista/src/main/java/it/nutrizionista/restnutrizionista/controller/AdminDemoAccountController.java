package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AccountDemoDto;
import it.nutrizionista.restnutrizionista.dto.ClienteAccountDemoDto;
import it.nutrizionista.restnutrizionista.dto.CreaAccountDemoRequest;
import it.nutrizionista.restnutrizionista.dto.EstendiAccountDemoRequest;
import it.nutrizionista.restnutrizionista.dto.ImpersonazioneDemoRequest;
import it.nutrizionista.restnutrizionista.dto.ImpersonazioneDemoResponse;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.RuotaPasswordDemoRequest;
import it.nutrizionista.restnutrizionista.enums.StatoAccountDemo;
import it.nutrizionista.restnutrizionista.service.AdminDemoAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Operazioni demo protette due volte: filter chain /api/admin/** e @PreAuthorize. */
@RestController
@RequestMapping("/api/admin/demo-accounts")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminDemoAccountController {
    @Autowired private AdminDemoAccountService service;

    @PostMapping
    public ResponseEntity<AccountDemoDto> crea(@Valid @RequestBody CreaAccountDemoRequest request,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.crea(request, http.getRemoteAddr(), http.getHeader("User-Agent")));
    }

    @GetMapping
    public PageResponse<AccountDemoDto> lista(
            @RequestParam(required = false) StatoAccountDemo stato,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.lista(stato, page, size);
    }

    @GetMapping("/{id}/clienti")
    public PageResponse<ClienteAccountDemoDto> listaClienti(
            @PathVariable Long id,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listaClienti(id, q, page, size);
    }

    @PostMapping("/{id}/disable")
    public AccountDemoDto disabilita(@PathVariable Long id, HttpServletRequest http) {
        return service.disabilita(id, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/enable")
    public AccountDemoDto riabilita(@PathVariable Long id, HttpServletRequest http) {
        return service.riabilita(id, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/extend")
    public AccountDemoDto estendi(@PathVariable Long id,
            @Valid @RequestBody EstendiAccountDemoRequest request, HttpServletRequest http) {
        return service.estendi(id, request.getGiorni(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/rotate-password")
    public AccountDemoDto ruotaPassword(@PathVariable Long id,
            @Valid @RequestBody RuotaPasswordDemoRequest request, HttpServletRequest http) {
        return service.ruotaPassword(id, request.getPassword(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/revoke-sessions")
    public AccountDemoDto revocaSessioni(@PathVariable Long id, HttpServletRequest http) {
        return service.revocaSessioni(id, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/{id}/impersonate")
    public ImpersonazioneDemoResponse impersona(@PathVariable Long id,
            @Valid @RequestBody ImpersonazioneDemoRequest request, HttpServletRequest http) {
        return service.impersona(id, request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }
}
