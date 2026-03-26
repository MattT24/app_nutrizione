package it.nutrizionista.restnutrizionista.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.service.OpenFoodFactsService;

/**
 * Controller proxy verso Open Food Facts.
 * Sottile: solo routing, @PreAuthorize e delega al Service.
 *
 * La ricerca usa Search-a-licious (Elasticsearch) via proxy backend
 * perché nessun endpoint OFF supporta CORS per chiamate browser.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/openfoodfacts")
public class OpenFoodFactsController {

    @Autowired private OpenFoodFactsService service;

    /** Ricerca prodotti su OFF via Search-a-licious. */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<String> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {

        String json = service.searchProducts(query, page, size);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    /** Importa un prodotto OFF nel catalogo personale del nutrizionista. */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('ALIMENTO_PERSONALE_CREATE')")
    public ResponseEntity<AlimentoBaseDto> importProduct(@RequestParam String barcode) {
        AlimentoBaseDto saved = service.importProduct(barcode);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
