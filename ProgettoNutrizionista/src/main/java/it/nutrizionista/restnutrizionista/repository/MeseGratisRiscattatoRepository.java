package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.MeseGratisRiscattato;

@Repository
public interface MeseGratisRiscattatoRepository extends JpaRepository<MeseGratisRiscattato, Long> {

    long countByNutrizionista_Id(Long nutrizionistaId);
}
