package it.nutrizionista.restnutrizionista.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Utente;

@Repository
public interface OrariStudioRepository extends JpaRepository<OrariStudio, Long> {
    
    // Restituisce TUTTI i giorni configurati per il nutrizionista
    List<OrariStudio> findByNutrizionista(Utente nutrizionista);
    
    // Cerca la singola riga di un giorno specifico (es. LUNEDI) per il nutrizionista
    Optional<OrariStudio> findByNutrizionistaAndGiornoSettimana(Utente nutrizionista, DayOfWeek giornoSettimana);
    
    // Utile per la validazione negli Appuntamenti (cerca per ID utente invece che per oggetto)
    Optional<OrariStudio> findByNutrizionista_IdAndGiornoSettimana(Long nutrizionistaId, DayOfWeek giornoSettimana);
    
    
    @Query("SELECT o FROM OrariStudio o WHERE o.nutrizionista.id = :nutrizionistaId AND o.giornoSettimana = :giorno")
    Optional<OrariStudio> findByNutrizionistaIdAndGiornoSettimana(
        @Param("nutrizionistaId") Long nutrizionistaId, 
        @Param("giorno") DayOfWeek giorno
    );
}