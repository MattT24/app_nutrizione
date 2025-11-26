package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Pasto;

public interface PastoRepository extends JpaRepository<Pasto, Long> {

}
