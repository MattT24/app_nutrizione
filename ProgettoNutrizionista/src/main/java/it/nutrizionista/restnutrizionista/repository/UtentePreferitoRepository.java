package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.entity.UtentePreferito;

@Repository
public interface UtentePreferitoRepository extends JpaRepository<UtentePreferito, Long> {
    
    List<UtentePreferito> findByUtenteIdOrderByCreatedAtDesc(Long utenteId);
    
    Optional<UtentePreferito> findByUtenteIdAndAlimentoId(Long utenteId, Long alimentoId);
    
    void deleteByUtenteIdAndAlimentoId(Long utenteId, Long alimentoId);
    
    boolean existsByUtenteIdAndAlimentoId(Long utenteId, Long alimentoId);
}
