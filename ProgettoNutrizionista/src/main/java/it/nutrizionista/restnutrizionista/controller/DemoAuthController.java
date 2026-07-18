package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.DemoLoginRequest;
import it.nutrizionista.restnutrizionista.dto.LoginResponse;
import it.nutrizionista.restnutrizionista.service.DemoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Endpoint pubblico separato: il login email/Google esistente resta invariato. */
@RestController
@RequestMapping("/api/auth/demo")
public class DemoAuthController {
    @Autowired private DemoAuthService demoAuthService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody DemoLoginRequest request,
            HttpServletRequest httpRequest) {
        // Non fidarsi direttamente di X-Forwarded-For: getRemoteAddr è corretto quando
        // Spring/proxy fidato sono configurati per i forwarded headers.
        return ResponseEntity.ok(demoAuthService.login(request, httpRequest.getRemoteAddr()));
    }
}
