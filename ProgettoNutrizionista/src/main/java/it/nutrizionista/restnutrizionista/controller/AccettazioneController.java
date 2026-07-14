package it.nutrizionista.restnutrizionista.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.nutrizionista.restnutrizionista.dto.AccettazioneDocumentoDto;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TipoDocumento;
import it.nutrizionista.restnutrizionista.service.AccettazioneService;
import it.nutrizionista.restnutrizionista.service.CurrentUserService;

/**
 * Gestione self-service delle accettazioni documenti (A4/A9) dell'utente loggato.
 * {@code /pending} + {@code /accetta} servono al gate di ri-accettazione (versione cambiata o utente
 * pre-esistente) — il gate FE al login è il pezzo di Wave 3.
 */
@RestController
@RequestMapping("/api/accettazioni")
@PreAuthorize("isAuthenticated()")
public class AccettazioneController {

    @Autowired
    private AccettazioneService accettazioneService;
    @Autowired
    private CurrentUserService currentUserService;

    /** Storico delle mie accettazioni. */
    @GetMapping("/me")
    public List<AccettazioneDocumentoDto> miei() {
        return accettazioneService.getMiei(currentUserService.getMe());
    }

    /** Documenti che devo (ri)accettare: versione corrente non ancora accettata (o mai accettata). */
    @GetMapping("/pending")
    public List<TipoDocumento> pending() {
        return accettazioneService.documentiDaAccettare(currentUserService.getMe());
    }

    /** Ri-accetta un insieme di documenti alla versione corrente; ritorna i pending residui. */
    @PostMapping("/accetta")
    public List<TipoDocumento> accetta(@RequestBody Set<TipoDocumento> tipi) {
        Utente me = currentUserService.getMe();
        accettazioneService.accetta(me, tipi);
        return accettazioneService.documentiDaAccettare(me);
    }

    /** Registra la revoca dell'accettazione di un documento (conseguenza a valle differita). */
    @PostMapping("/me/{tipo}/revoca")
    public void revoca(@PathVariable TipoDocumento tipo) {
        accettazioneService.revoca(currentUserService.getMe(), tipo);
    }
}
