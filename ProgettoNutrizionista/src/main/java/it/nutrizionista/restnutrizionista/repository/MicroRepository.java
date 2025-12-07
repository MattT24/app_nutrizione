package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Micro;

public interface MicroRepository extends JpaRepository<Micro, Long> {

	Optional<Micro> findByNome(String nome);

}
