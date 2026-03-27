package it.nutrizionista.restnutrizionista.repository;

import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoFascicoloRepository extends JpaRepository<DocumentoFascicolo, Long> {
    List<DocumentoFascicolo> findByClienteIdOrderByDataCreazioneDesc(Long clienteId);
    Optional<DocumentoFascicolo> findByClienteIdAndTipoDocumentoAndRiferimentoId(Long clienteId, TipoDocumento tipoDocumento, Long riferimentoId);
}
