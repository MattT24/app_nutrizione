package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import jakarta.validation.Valid;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{

	Optional<Cliente> findByNome(@Valid String nome);

	Optional<Cliente> findByCognome(@Valid String cognome);

	Page<Cliente> findByNutrizionistaId(Long nutrizionistaId, Pageable pageable);

}
