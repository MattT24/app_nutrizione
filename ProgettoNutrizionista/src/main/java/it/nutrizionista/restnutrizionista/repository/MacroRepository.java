package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Macro;

public interface MacroRepository extends JpaRepository<Macro, Long> {

}
