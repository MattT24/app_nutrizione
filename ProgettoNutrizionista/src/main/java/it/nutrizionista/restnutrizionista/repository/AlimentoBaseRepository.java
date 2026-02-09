package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import jakarta.validation.Valid;

public interface AlimentoBaseRepository extends JpaRepository<AlimentoBase, Long>{

	Optional<AlimentoBase> findByNome(@Valid String nome);

	List<AlimentoBase> findByNomeContainingIgnoreCase(String query);

	@Query("""
		SELECT a
		FROM AlimentoBase a
		WHERE lower(a.nome) LIKE concat('%', lower(:query), '%')
		ORDER BY
			CASE
				WHEN lower(a.nome) LIKE concat(lower(:query), '%') THEN 0
				ELSE 1
			END,
			lower(a.nome) ASC
	""")
	List<AlimentoBase> searchByNomeRanked(@Param("query") String query);

}
