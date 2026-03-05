package it.nutrizionista.restnutrizionista.repository;

import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalcoloTdeeRepository extends JpaRepository<CalcoloTdee, Long> {
	// Trova lo storico
    List<CalcoloTdee> findByClienteIdOrderByDataCalcoloDesc(Long clienteId);

    // NUOVO: Elimina tutti i calcoli associati a un cliente
    void deleteByClienteId(Long clienteId);
}