package it.nutrizionista.restnutrizionista.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.ObiettivoNutrizionale;

public interface ObiettivoNutrizionaleRepository extends JpaRepository<ObiettivoNutrizionale, Long> {

	Optional<ObiettivoNutrizionale> findByCliente_Id(Long clienteId);

	Optional<ObiettivoNutrizionale> findByCliente_IdAndCliente_Nutrizionista_Id(Long clienteId, Long nutrizionistaId);

	void deleteByCliente_Id(Long clienteId);
}
