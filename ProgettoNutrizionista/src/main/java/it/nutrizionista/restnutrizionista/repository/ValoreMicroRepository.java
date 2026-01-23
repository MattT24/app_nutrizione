package it.nutrizionista.restnutrizionista.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.nutrizionista.restnutrizionista.entity.ValoreMicro;

public interface ValoreMicroRepository extends JpaRepository<ValoreMicro, Long> {

	
	@Query("""
	    SELECT vm
	    FROM ValoreMicro vm
	    JOIN FETCH vm.micronutriente
	    WHERE vm.alimento.id = :id
	""")
	Set<ValoreMicro> findByAlimento_Id(Long id);
}
