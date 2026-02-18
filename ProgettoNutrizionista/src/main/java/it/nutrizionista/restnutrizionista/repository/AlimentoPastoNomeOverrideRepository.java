package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;

public interface AlimentoPastoNomeOverrideRepository extends JpaRepository<AlimentoPastoNomeOverride, Long> {
	Optional<AlimentoPastoNomeOverride> findByAlimentoPasto_Id(Long alimentoPastoId);
	void deleteByAlimentoPasto_Id(Long alimentoPastoId);
	boolean existsByAlimentoPasto_Id(Long alimentoPastoId);
}

