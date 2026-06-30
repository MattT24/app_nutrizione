package it.nutrizionista.restnutrizionista.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.BadgeSbloccato;

@Repository
public interface BadgeSbloccatoRepository extends JpaRepository<BadgeSbloccato, Long> {

    List<BadgeSbloccato> findByNutrizionista_Id(Long nutrizionistaId);
}
