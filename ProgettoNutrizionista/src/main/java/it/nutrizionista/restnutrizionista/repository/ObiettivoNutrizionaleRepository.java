package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.ObiettivoNutrizionale;

public interface ObiettivoNutrizionaleRepository extends JpaRepository<ObiettivoNutrizionale, Long> {

	Optional<ObiettivoNutrizionale> findByCliente_IdAndAttivoTrue(Long clienteId);

	List<ObiettivoNutrizionale> findByCliente_IdOrderByDataCreazioneDesc(Long clienteId);

	void deleteByIdAndCliente_Id(Long id, Long clienteId);
}
