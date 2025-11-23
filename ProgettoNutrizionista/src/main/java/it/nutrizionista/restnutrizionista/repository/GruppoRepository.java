package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Gruppo;

/** Repository CRUD per Gruppo. */
public interface GruppoRepository extends JpaRepository<Gruppo, Long> { }
