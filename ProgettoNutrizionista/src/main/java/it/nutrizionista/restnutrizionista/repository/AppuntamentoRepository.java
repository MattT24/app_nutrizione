package it.nutrizionista.restnutrizionista.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;

@Repository
public interface AppuntamentoRepository extends JpaRepository<Appuntamento, Long> {

    List<Appuntamento> findByCliente(Cliente cliente);

    List<Appuntamento> findByNutrizionista(Utente nutrizionista);

    List<Appuntamento> findByData(LocalDate data);

    List<Appuntamento> findByStato(Appuntamento.StatoAppuntamento stato);

    List<Appuntamento> findByClienteAndData(Cliente cliente, LocalDate data);

    List<Appuntamento> findByNutrizionistaAndDataBetween(Utente nutrizionista, LocalDate dataInizio, LocalDate dataFine);

    boolean existsByNutrizionistaAndDataAndOra(Utente nutrizionista, LocalDate data, LocalTime ora);
}