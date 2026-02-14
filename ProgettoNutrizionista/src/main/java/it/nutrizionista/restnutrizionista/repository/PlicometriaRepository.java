package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.Plicometria;

public interface PlicometriaRepository extends JpaRepository<Plicometria, Long> {
	Page<Plicometria> findByCliente_IdOrderByDataMisurazioneDesc(Long clienteId, Pageable pageable);

	Optional<Plicometria> findByIdAndCliente_Nutrizionista_Id(Long id, Long nutrizionistaId);
}
