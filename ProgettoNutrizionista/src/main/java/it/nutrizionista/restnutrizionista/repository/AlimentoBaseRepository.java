package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import jakarta.validation.Valid;

public interface AlimentoBaseRepository extends JpaRepository<AlimentoBase, Long>{

	Optional<AlimentoBase> findByNome(@Valid String nome);

	List<AlimentoBase> findByNomeContainingIgnoreCase(String query);

}
