package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import it.nutrizionista.restnutrizionista.entity.AlimentoDaEvitare;


public interface AlimentoDaEvitareRepository extends JpaRepository<AlimentoDaEvitare, Long>{

	boolean existsByCliente_IdAndAlimento_Id(Long clienteId, Long alimentoId);
	Optional<AlimentoDaEvitare> findByCliente_IdAndAlimento_Id(Long clienteId, Long alimentoId);
	List<AlimentoDaEvitare> findByCliente_IdAndAlimento_IdIn(Long clienteId, List<Long> alimentoIds);
	

}
