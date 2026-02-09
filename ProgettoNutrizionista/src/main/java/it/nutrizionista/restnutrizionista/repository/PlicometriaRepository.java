package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Plicometria;

public interface PlicometriaRepository extends JpaRepository<Plicometria, Long> {
	Page<Plicometria> findByCliente_IdOrderByDataMisurazioneDesc(Long clienteId, Pageable pageable);
}
