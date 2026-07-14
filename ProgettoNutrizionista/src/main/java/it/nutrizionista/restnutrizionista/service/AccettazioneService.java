package it.nutrizionista.restnutrizionista.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AccettazioneDocumentoDto;
import it.nutrizionista.restnutrizionista.entity.AccettazioneDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TipoDocumento;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AccettazioneDocumentoRepository;

/**
 * Gestione delle accettazioni dei documenti legali (A4/A9): registrazione, calcolo dei documenti
 * ancora da accettare (versione cambiata o utente pre-esistente), ri-accettazione e revoca.
 * Le versioni correnti dei documenti hanno un default in codice (placeholder finché non arrivano i
 * testi legali, Wave 3) e sono overridabili via property {@code app.accettazione.versione.*}.
 */
@Service
public class AccettazioneService {

    /** Documenti che ogni nutrizionista deve accettare per usare la piattaforma. */
    private static final Set<TipoDocumento> RICHIESTI = EnumSet.allOf(TipoDocumento.class);

    private final AccettazioneDocumentoRepository repo;

    @Value("${app.accettazione.versione.privacy:v1-draft}")
    private String versionePrivacy;
    @Value("${app.accettazione.versione.termini:v1-draft}")
    private String versioneTermini;
    @Value("${app.accettazione.versione.dpa:v1-draft}")
    private String versioneDpa;

    public AccettazioneService(AccettazioneDocumentoRepository repo) {
        this.repo = repo;
    }

    /** Versione corrente del documento (placeholder finché non arrivano i testi legali di Wave 3). */
    public String versioneCorrente(TipoDocumento tipo) {
        return switch (tipo) {
            case PRIVACY_POLICY -> versionePrivacy;
            case TERMINI_SERVIZIO -> versioneTermini;
            case DPA -> versioneDpa;
        };
    }

    /** Registra tutte le accettazioni obbligatorie alla registrazione (versione corrente). Stessa tx del register. */
    @Transactional
    public void registraAccettazioniDiRegistrazione(Utente utente) {
        accetta(utente, RICHIESTI);
    }

    /** Registra un'accettazione (alla versione corrente) per ciascun tipo indicato — ri-accettazione / utenti pre-esistenti. */
    @Transactional
    public void accetta(Utente utente, Set<TipoDocumento> tipi) {
        Instant now = Instant.now();
        for (TipoDocumento tipo : tipi) {
            repo.save(new AccettazioneDocumento(utente, tipo, versioneCorrente(tipo), now));
        }
    }

    /**
     * Documenti che l'utente deve (ri)accettare: per ogni documento richiesto, manca un'accettazione
     * non revocata ALLA VERSIONE CORRENTE. Vuoto = tutto in regola. È ciò che rende il versioning
     * effettivo (al bump della versione l'utente rientra tra i "pending") e copre gli utenti pre-esistenti.
     */
    @Transactional(readOnly = true)
    public List<TipoDocumento> documentiDaAccettare(Utente utente) {
        List<TipoDocumento> mancanti = new ArrayList<>();
        for (TipoDocumento tipo : RICHIESTI) {
            var attiva = repo.findFirstByUtente_IdAndTipoAndRevocatoAtIsNullOrderByAccettatoAtDesc(utente.getId(), tipo);
            if (attiva.isEmpty() || !versioneCorrente(tipo).equals(attiva.get().getVersione())) {
                mancanti.add(tipo);
            }
        }
        return mancanti;
    }

    @Transactional(readOnly = true)
    public List<AccettazioneDocumentoDto> getMiei(Utente utente) {
        return repo.findByUtente_IdOrderByAccettatoAtDesc(utente.getId())
                .stream().map(DtoMapper::toAccettazioneDto).toList();
    }

    /** Registra la revoca dell'accettazione attiva del tipo indicato (se presente). */
    @Transactional
    public void revoca(Utente utente, TipoDocumento tipo) {
        repo.findFirstByUtente_IdAndTipoAndRevocatoAtIsNullOrderByAccettatoAtDesc(utente.getId(), tipo)
                .ifPresent(a -> {
                    a.revoca(Instant.now());
                    repo.save(a);
                });
    }
}
