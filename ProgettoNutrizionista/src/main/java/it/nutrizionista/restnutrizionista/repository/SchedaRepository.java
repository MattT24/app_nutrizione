package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Scheda;

public interface SchedaRepository extends JpaRepository<Scheda, Long> {

}
