package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;

public interface MisurazioneAntropometricaRepository extends JpaRepository<MisurazioneAntropometrica, Long> {

	Page<MisurazioneAntropometrica> findByCliente_IdOrderByDataMisurazioneDesc(Long id, Pageable pageable);

	Optional<MisurazioneAntropometrica> findByIdAndCliente_Nutrizionista_Id(Long id, Long nutrizionistaId);
}
