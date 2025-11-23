package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Micro;

public interface MicroRepository extends JpaRepository<Micro, Long> {

}
