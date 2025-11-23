package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;

public interface AlimentoPastoRepository extends JpaRepository<AlimentoPasto, Long> {

}
