package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Macro;

public interface MacroRepository extends JpaRepository<Macro, Long> {

	Optional<Macro> findByAlimento_Id(Long id);

}
