package it.nutrizionista.restnutrizionista.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AppuntamentoDto;
import it.nutrizionista.restnutrizionista.dto.AppuntamentoFormDto;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.service.AppuntamentoService;

@RestController
@RequestMapping("/api/appuntamenti")
public class AppuntamentoController {

    @Autowired
    private AppuntamentoService appuntamentoService;

    /**
     * Crea un nuovo appuntamento (usa l'utente autenticato come nutrizionista)
     * POST /api/appuntamenti                  //un appuntamento associato allo stesso utente, NON puo avere la stessa data
     */
    @PostMapping													 //rotta testata e funzionante su postman
    public ResponseEntity<AppuntamentoDto> createAppuntamento(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AppuntamentoFormDto formDto) {
        try {
            String email = userDetails.getUsername(); // Recupera l'email (username) dal token JWT
            AppuntamentoDto created = appuntamentoService.createAppuntamento(email, formDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Aggiorna un appuntamento esistente						 //rotta testata e funzionante su postman
     * PUT /api/appuntamenti/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppuntamentoDto> updateAppuntamento(
            @PathVariable Long id,
            @RequestBody AppuntamentoFormDto formDto) {
        try {
            AppuntamentoDto updated = appuntamentoService.updateAppuntamento(id, formDto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Recupera un appuntamento per ID
     * GET /api/appuntamenti/{id}               //questo metodo, utile per l' admin, non so se serve in realtà
     */
    @GetMapping("/{id}")							//rotta testata e funzionante su postman
    public ResponseEntity<AppuntamentoDto> getAppuntamentoById(@PathVariable Long id) {
        try {
            AppuntamentoDto appuntamento = appuntamentoService.getAppuntamentoById(id);
            return ResponseEntity.ok(appuntamento);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Recupera tutti gli appuntamenti
     * GET /api/appuntamenti            //rotta testata e funzionante su postman
     */
    @GetMapping
    public ResponseEntity<List<AppuntamentoDto>> getAllAppuntamenti() {
        List<AppuntamentoDto> appuntamenti = appuntamentoService.getAllAppuntamenti();
        return ResponseEntity.ok(appuntamenti);
    }

    /**
     * Recupera gli appuntamenti di un cliente
     * GET /api/appuntamenti/cliente/{clienteId}						//rotta testata e funzionante su postman
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<AppuntamentoDto>> getAppuntamentiByCliente(@PathVariable Long clienteId) {
        try {
            List<AppuntamentoDto> appuntamenti = appuntamentoService.getAppuntamentiByCliente(clienteId);
            return ResponseEntity.ok(appuntamenti);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    
    /**
     * Recupera gli appuntamenti del nutrizionista autenticato
     * GET /api/appuntamenti/nutrizionista/me							//rotta testata e funzionante su postman
     */
    @GetMapping("/nutrizionista/me")
    public ResponseEntity<List<AppuntamentoDto>> getMyAppuntamenti(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String email = userDetails.getUsername(); // Recupera l'email (username) dal token JWT
            List<AppuntamentoDto> appuntamenti = appuntamentoService.getAppuntamentiByNutrizionista(email);
            return ResponseEntity.ok(appuntamenti);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Recupera gli appuntamenti per data
     * GET /api/appuntamenti/data?data=2024-01-15							//rotta testata e funzionante su postman
     */
    @GetMapping("/data")
    public ResponseEntity<List<AppuntamentoDto>> getAppuntamentiByData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<AppuntamentoDto> appuntamenti = appuntamentoService.getAppuntamentiByData(data);
        return ResponseEntity.ok(appuntamenti);
    }

    /**
     * Recupera gli appuntamenti per stato
     * GET /api/appuntamenti/stato?stato=PROGRAMMATO				//rotta testata e funzionante su postman
     */
    @GetMapping("/stato")
    public ResponseEntity<List<AppuntamentoDto>> getAppuntamentiByStato(
            @RequestParam Appuntamento.StatoAppuntamento stato) {
        List<AppuntamentoDto> appuntamenti = appuntamentoService.getAppuntamentiByStato(stato);
        return ResponseEntity.ok(appuntamenti);
    }

    /**
     * Recupera gli appuntamenti del nutrizionista autenticato in un range di date
     * GET /api/appuntamenti/nutrizionista/me/range?dataInizio=2024-01-01&dataFine=2024-01-31		//rotta testata e funzionante su postman
     */																								//avrebbe senso metterli in ordine di data crescente
    @GetMapping("/nutrizionista/me/range")
    public ResponseEntity<List<AppuntamentoDto>> getMyAppuntamentiByDateRange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInizio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFine) {
        try {
            String email = userDetails.getUsername(); // Recupera l'email (username) dal token JWT
            List<AppuntamentoDto> appuntamenti = appuntamentoService
                    .getAppuntamentiByNutricionistaAndDateRange(email, dataInizio, dataFine);
            return ResponseEntity.ok(appuntamenti);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Cambia lo stato di un appuntamento								//rotta testata e funzionante su postman
     * PATCH /api/appuntamenti/{id}/stato?stato=CONFERMATO
     */
    @PatchMapping("/{id}/stato")
    public ResponseEntity<AppuntamentoDto> cambiaStatoAppuntamento(
            @PathVariable Long id,
            @RequestParam Appuntamento.StatoAppuntamento stato) {
        try {
            AppuntamentoDto updated = appuntamentoService.cambiaStatoAppuntamento(id, stato);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Elimina un appuntamento
     * DELETE /api/appuntamenti/{id}  prima di eliminare un appuntamento bisogna cambiare lo stato in "ANNULLATO"   //rotta testata e funzionante su postman
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppuntamento(@PathVariable Long id) {
        try {
            appuntamentoService.deleteAppuntamento(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}