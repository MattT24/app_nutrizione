package it.nutrizionista.restnutrizionista.repository;

import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalcoloTdeeRepository extends JpaRepository<CalcoloTdee, Long> {
	// Trova lo storico
    List<CalcoloTdee> findByClienteIdOrderByDataCalcoloDesc(Long clienteId);

    // NUOVO: Elimina tutti i calcoli associati a un cliente
    void deleteByClienteId(Long clienteId);

    // Lookup scoped per ownership (usato da OwnershipValidator.getOwnedCalcoloTdee)
    Optional<CalcoloTdee> findByIdAndCliente_Nutrizionista_Id(Long id, Long nutrizionistaId);

    // Ultimi 10 calcoli del SOLO nutrizionista corrente (scoped anti-leakage cross-tenant)
    List<CalcoloTdee> findTop10ByCliente_Nutrizionista_IdOrderByIdDesc(Long nutrizionistaId);
}