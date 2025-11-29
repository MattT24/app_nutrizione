package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import jakarta.validation.Valid;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{

	Optional<Cliente> findByNome(@Valid String nome);

}
