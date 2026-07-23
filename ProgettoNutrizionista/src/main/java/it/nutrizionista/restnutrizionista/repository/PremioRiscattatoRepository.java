package it.nutrizionista.restnutrizionista.repository;

import java.time.Instant;

import it.nutrizionista.restnutrizionista.entity.PremioRiscattato;
import it.nutrizionista.restnutrizionista.enums.TipoPremio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PremioRiscattatoRepository extends JpaRepository<PremioRiscattato, Long> {

    /** F-USER-DEL: cancellazione account — premi riscattati del nutrizionista. */
    void deleteByNutrizionista_Id(Long nutrizionistaId);

    long countByNutrizionista_IdAndTipoPremio(Long nutrizionistaId, TipoPremio tipoPremio);

    boolean existsByNutrizionista_IdAndTipoPremioAndDataRiscattoGreaterThanEqual(
            Long nutrizionistaId, TipoPremio tipoPremio, Instant inizioMese);
}
