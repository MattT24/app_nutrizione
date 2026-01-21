package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import jakarta.validation.Valid;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{
    
    // Cerca per ID e Nutrizionista (per sicurezza rapida)
    Optional<Cliente> findByIdAndNutrizionistaId(Long id, Long nutrizionistaId);

    // Cerca paginata per nutrizionista
    Page<Cliente> findByNutrizionistaId(Long nutrizionistaId, Pageable pageable);
    
    // Ricerca parziale (es. "Mar" trova "Mario") limitata al nutrizionista
    List<Cliente> findByNutrizionistaIdAndNomeContainingIgnoreCase(Long nutrizionistaId, String nome);
    
    List<Cliente> findByNutrizionistaIdAndCognomeContainingIgnoreCase(Long nutrizionistaId, String cognome);
    
    // Verifica duplicati codice fiscale
    boolean existsByCodiceFiscale(String codiceFiscale);
}