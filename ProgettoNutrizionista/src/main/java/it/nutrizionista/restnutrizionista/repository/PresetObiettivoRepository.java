package it.nutrizionista.restnutrizionista.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.PresetObiettivo;

public interface PresetObiettivoRepository extends JpaRepository<PresetObiettivo, Long> {

	List<PresetObiettivo> findByNutrizionista_IdOrderByNomeAsc(Long nutrizionistaId);

	void deleteByIdAndNutrizionista_Id(Long id, Long nutrizionistaId);
}
