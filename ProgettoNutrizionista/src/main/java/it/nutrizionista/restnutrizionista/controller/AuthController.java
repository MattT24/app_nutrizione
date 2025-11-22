package it.nutrizionista.restnutrizionista.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.RegisterRequest;
import it.nutrizionista.restnutrizionista.dto.ResetPasswordDto;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.dto.LoginRequest;
import it.nutrizionista.restnutrizionista.dto.LoginResponse;
import it.nutrizionista.restnutrizionista.service.AuthService;

/** Controller REST per autenticazione: delega tutta la logica ad AuthService. */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    	var dto = authService.login(req);
    	return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.noContent().build();
    }
}
