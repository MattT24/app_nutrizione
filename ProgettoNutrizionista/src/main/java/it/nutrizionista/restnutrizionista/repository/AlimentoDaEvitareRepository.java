package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;

public interface AlimentoDaEvitareRepository extends JpaRepository<AlimentoDaEvitare, Long>{

	boolean existsByCliente_IdAndAlimento_Id(Long clienteId, Long alimentoId);
	
    // Fondamentale per la lista nel profilo cliente
    Page<AlimentoDaEvitare> findByCliente_Id(Long clienteId, Pageable pageable);
    
	List<AlimentoDaEvitare> findByCliente_IdAndAlimento_IdIn(Long clienteId, List<Long> alimentoIds);

	Optional<AlimentoDaEvitare> findByCliente_IdAndAlimento_Id(Long clienteId, Long id);
}