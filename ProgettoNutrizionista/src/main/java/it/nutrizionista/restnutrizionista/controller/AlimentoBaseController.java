package it.nutrizionista.restnutrizionista.controller;

import java.util.List; // Import List

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // Importa tutto

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseDto;
import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.service.AlimentoBaseService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/alimenti_base")
public class AlimentoBaseController {

	@Autowired private AlimentoBaseService service;
	
	@PostMapping
	@PreAuthorize("hasAuthority('ALIMENTO_CREATE')") // Solo Admin
	public ResponseEntity<AlimentoBaseDto> add(@Valid @RequestBody AlimentoBaseFormDto form){
		var create = service.create(form);
		return ResponseEntity.status(201).body(create);
	}

	@PostMapping("/personale")
	@PreAuthorize("hasAuthority('ALIMENTO_PERSONALE_CREATE')") // Nutrizionista
	public ResponseEntity<AlimentoBaseDto> addPersonale(@Valid @RequestBody AlimentoBaseFormDto form){
		var create = service.createPersonale(form);
		return ResponseEntity.status(201).body(create);
	}
	
	@PutMapping
	@PreAuthorize("hasAuthority('ALIMENTO_UPDATE')")
	public ResponseEntity<AlimentoBaseDto> update(@Valid @RequestBody AlimentoBaseFormDto form){
		var updated = service.update(form);
		return ResponseEntity.status(201).body(updated); // O 200 OK
	}

	@DeleteMapping("/{id}") // PathVariable per ID
	@PreAuthorize("hasAuthority('ALIMENTO_DELETE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/personale/{id}")
	@PreAuthorize("hasAuthority('ALIMENTO_PERSONALE_CREATE')")
	public ResponseEntity<Void> deletePersonale(@PathVariable Long id) {
		service.deletePersonale(id);
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	@PreAuthorize("hasAuthority('ALIMENTO_READ')")
	public ResponseEntity<PageResponse<AlimentoBaseDto>> allAlimentiBase(Pageable pageable){
		return ResponseEntity.ok(service.listAll(pageable));
	}
	
    // NUOVO: Endpoint di ricerca per il frontend
    // GET /api/alimenti_base/search?query=Pollo
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<List<AlimentoBaseDto>> search(@RequestParam String query) {
        return ResponseEntity.ok(service.search(query));
    }

    @GetMapping("/piu-utilizzati")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<List<AlimentoBaseDto>> getTopAlimenti(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(service.getTopAlimenti(limit));
    }

	/* ==========================================================
	 * GESTIONE PREFERITI
	 * ========================================================== */

    @GetMapping("/preferiti")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<List<AlimentoBaseDto>> getPreferiti() {
        return ResponseEntity.ok(service.getPreferiti());
    }

    @PostMapping("/preferiti/{id}")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<Void> addPreferito(@PathVariable Long id) {
        service.addPreferito(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/preferiti/{id}")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<Void> removePreferito(@PathVariable Long id) {
        service.removePreferito(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categorie")
    @PreAuthorize("hasAuthority('ALIMENTO_READ')")
    public ResponseEntity<List<String>> getCategorie() {
        return ResponseEntity.ok(service.getCategorie());
    }

	@GetMapping("/{id}/macro")
	@PreAuthorize("hasAuthority('ALIMENTO_READ')") // Uniformato permesso
	public ResponseEntity<AlimentoBaseDto> dettaglioMacro(@PathVariable Long id){
		var dto = service.dettaglioMacro(id);
		return ResponseEntity.ok(dto);
	}
	
	@GetMapping("/{id}/dettaglio")
	@PreAuthorize("hasAuthority('ALIMENTO_READ')")
	public ResponseEntity<AlimentoBaseDto> dettaglio(@PathVariable Long id){
		var dto = service.dettaglio(id);
		return ResponseEntity.ok(dto);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ALIMENTO_READ')")
	public ResponseEntity<AlimentoBaseDto> getById(@PathVariable Long id){
		var dto = service.getById(id);
		return ResponseEntity.ok(dto);
	}
	
    // Modificato per usare RequestParam invece di Body
	@GetMapping("/byNome")
	@PreAuthorize("hasAuthority('ALIMENTO_READ')")
	public ResponseEntity<AlimentoBaseDto> getByNome(@RequestParam String nome){
		var dto = service.getByNome(nome);
		return ResponseEntity.ok(dto);
	 }
}