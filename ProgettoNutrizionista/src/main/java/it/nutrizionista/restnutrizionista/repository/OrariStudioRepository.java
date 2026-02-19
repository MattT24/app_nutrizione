package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Utente;

@Repository
public interface OrariStudioRepository extends JpaRepository<OrariStudio, Long> {

    Optional<OrariStudio> findByNutrizionista(Utente nutrizionista);

    boolean existsByNutrizionista(Utente nutrizionista);
}
