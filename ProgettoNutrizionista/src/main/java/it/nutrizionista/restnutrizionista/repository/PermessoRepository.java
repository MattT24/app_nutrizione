package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Permesso;

import java.util.List;
import java.util.Optional;

/** Repository CRUD per Permesso. */
public interface PermessoRepository extends JpaRepository<Permesso, Long> {
    List<Permesso> findByGruppo_Id(Long gruppoId);
    Optional<Permesso> findByAlias(String alias);
}
