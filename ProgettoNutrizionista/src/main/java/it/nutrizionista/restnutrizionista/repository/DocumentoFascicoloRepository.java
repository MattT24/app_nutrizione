package it.nutrizionista.restnutrizionista.repository;

import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoFascicoloRepository extends JpaRepository<DocumentoFascicolo, Long> {
    List<DocumentoFascicolo> findByClienteIdOrderByDataCreazioneDesc(Long clienteId);
}
