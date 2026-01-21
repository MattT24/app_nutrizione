package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;

public interface MisurazioneAntropometricaRepository extends JpaRepository<MisurazioneAntropometrica, Long> {

	Page<MisurazioneAntropometrica> findByClienteId(Long id, Pageable pageable);

}
