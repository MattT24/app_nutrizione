package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;
import jakarta.validation.Valid;

public interface AlimentoDaEvitareRepository extends JpaRepository<AlimentoDaEvitare, Long>{

	Optional<AlimentoDaEvitare> findByNome(@Valid String nome);

}
