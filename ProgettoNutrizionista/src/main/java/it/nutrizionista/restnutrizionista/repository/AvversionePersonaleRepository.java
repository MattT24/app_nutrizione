package it.nutrizionista.restnutrizionista.repository;

import java.util.Set;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;

@Repository
public interface AvversionePersonaleRepository extends JpaRepository<AvversionePersonale, Long> {

    @Query("SELECT a FROM AvversionePersonale a JOIN FETCH a.alimento WHERE a.cliente.id = :clienteId")
    Set<AvversionePersonale> findByClienteIdWithAlimenti(@Param("clienteId") Long clienteId);
    
    Optional<AvversionePersonale> findByClienteIdAndAlimentoId(Long clienteId, Long alimentoId);
}
