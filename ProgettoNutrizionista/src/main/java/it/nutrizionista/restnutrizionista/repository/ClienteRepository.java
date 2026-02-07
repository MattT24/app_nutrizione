package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{
    
    // Cerca per ID e Nutrizionista (per sicurezza rapida)
    Optional<Cliente> findByIdAndNutrizionista_Id(Long id, Long nutrizionistaId);

    // Cerca paginata per nutrizionista
    Page<Cliente> findByNutrizionista_Id(Long nutrizionistaId, Pageable pageable);
    
    // Ricerca parziale (es. "Mar" trova "Mario") limitata al nutrizionista
    List<Cliente> findByNutrizionista_IdAndNomeContainingIgnoreCase(Long nutrizionistaId, String nome);
    
    List<Cliente> findByNutrizionista_IdAndCognomeContainingIgnoreCase(Long nutrizionistaId, String cognome);
    
    // Verifica duplicati codice fiscale
    boolean existsByCodiceFiscale(String codiceFiscale);
    
    @Query("""
    	    select c from Cliente c
    	    where c.nutrizionista.id = :nutrizionistaId
    	      and (
    	        lower(c.nome) like lower(concat('%', :q, '%'))
    	        or lower(c.cognome) like lower(concat('%', :q, '%'))
    	      )
    	    order by c.cognome asc, c.nome asc
    	""")
    	List<Cliente> searchMyClientsByName(
    	    @Param("nutrizionistaId") Long nutrizionistaId,
    	    @Param("q") String q
    	);
    //Questo garantisce che il nutrizionista vede solo i suoi clienti
    @Query("select c from Cliente c where c.id = :id and c.nutrizionista.id = :nutrizionistaId")
    Cliente findMineById(@Param("id") Long id, @Param("nutrizionistaId") Long nutrizionistaId);


}