package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Ruolo;

/** Repository CRUD per Ruolo. */
public interface RuoloRepository extends JpaRepository<Ruolo, Long> {
    Optional<Ruolo> findByAlias(String alias);
}
