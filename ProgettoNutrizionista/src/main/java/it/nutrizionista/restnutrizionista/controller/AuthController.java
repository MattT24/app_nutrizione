package it.nutrizionista.restnutrizionista.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.RegisterRequest;
import it.nutrizionista.restnutrizionista.dto.LoginRequest;
import it.nutrizionista.restnutrizionista.dto.LoginResponse;
import it.nutrizionista.restnutrizionista.dto.GoogleIdTokenRequest;
import it.nutrizionista.restnutrizionista.dto.GoogleRegisterRequest;
import it.nutrizionista.restnutrizionista.dto.GoogleAuthResponse;
import it.nutrizionista.restnutrizionista.service.AuthService;

/** Controller REST per autenticazione: delega tutta la logica ad AuthService. */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
    	var dto = authService.login(req, clientIp(request));
    	return (dto == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    /** IP del client per il rate limiting: primo valore di X-Forwarded-For se dietro proxy, altrimenti remoteAddr. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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

    /** Login con Google: se l'utente non esiste ancora, risponde con registrationRequired=true. */
    @PostMapping("/google/login")
    public ResponseEntity<GoogleAuthResponse> googleLogin(@Valid @RequestBody GoogleIdTokenRequest req) {
        return ResponseEntity.ok(authService.loginOrPrepareRegisterWithGoogle(req));
    }

    /** Completa la registrazione avviata con Google con i campi obbligatori mancanti. */
    @PostMapping("/google/register")
    public ResponseEntity<LoginResponse> googleRegister(@Valid @RequestBody GoogleRegisterRequest req) {
        return ResponseEntity.ok(authService.registerWithGoogle(req));
    }
}
