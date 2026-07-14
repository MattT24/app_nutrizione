package it.nutrizionista.restnutrizionista.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.nutrizionista.restnutrizionista.entity.AccettazioneDocumento;
import it.nutrizionista.restnutrizionista.enums.TipoDocumento;

@Repository
public interface AccettazioneDocumentoRepository extends JpaRepository<AccettazioneDocumento, Long> {

    /** Storico accettazioni dell'utente (più recenti prima). */
    List<AccettazioneDocumento> findByUtente_IdOrderByAccettatoAtDesc(Long utenteId);

    /** Accettazione ATTIVA (non revocata) più recente di un dato tipo — per calcolo pending e revoca. */
    Optional<AccettazioneDocumento> findFirstByUtente_IdAndTipoAndRevocatoAtIsNullOrderByAccettatoAtDesc(
            Long utenteId, TipoDocumento tipo);
}
