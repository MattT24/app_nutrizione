package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;

public interface AlimentoPastoRepository extends JpaRepository<AlimentoPasto, Long> {

	boolean existsByPasto_IdAndAlimento_Id(Long id, Long id2);

	void deleteByPasto_IdAndAlimento_Id(Long id, Long id2);

	List<AlimentoPasto> findByPasto_Id(Long pastoId);

	Optional<AlimentoPasto> findByPasto_IdAndAlimento_Id(Long id, Long id2);

	Optional<AlimentoPasto> findByIdAndPasto_Scheda_Cliente_Nutrizionista_Id(Long alimentoPastoId, Long nutrizionistaId);

}
