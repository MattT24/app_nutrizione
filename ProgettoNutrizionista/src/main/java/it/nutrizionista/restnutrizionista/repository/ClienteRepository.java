package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{
    
    // Cerca per ID e Nutrizionista (per sicurezza rapida)
    Optional<Cliente> findByIdAndNutrizionista_Id(Long id, Long nutrizionistaId);

    // Cerca paginata per nutrizionista
    Page<Cliente> findByNutrizionista_Id(Long nutrizionistaId, Pageable pageable);
    
    // Ricerca parziale (es. "Mar" trova "Mario") limitata al nutrizionista
    List<Cliente> findByNutrizionista_IdAndNomeContainingIgnoreCase(Long nutrizionistaId, String nome);
    
    List<Cliente> findByNutrizionista_IdAndCognomeContainingIgnoreCase(Long nutrizionistaId, String cognome);
    
    // Verifica duplicati codice fiscale
    boolean existsByCodiceFiscale(String codiceFiscale);
}