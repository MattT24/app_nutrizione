package it.nutrizionista.restnutrizionista.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.ConteggiAssistenzaDto;
import it.nutrizionista.restnutrizionista.dto.CreaMessaggioAssistenzaRequest;
import it.nutrizionista.restnutrizionista.dto.CreaTicketAssistenzaRequest;
import it.nutrizionista.restnutrizionista.dto.MessaggioAssistenzaDto;
import it.nutrizionista.restnutrizionista.dto.NutrizionistaAssistenzaDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.PollingMessaggiAssistenzaDto;
import it.nutrizionista.restnutrizionista.dto.TicketAssistenzaDto;
import it.nutrizionista.restnutrizionista.entity.MessaggioAssistenza;
import it.nutrizionista.restnutrizionista.entity.TicketAssistenza;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.StatoTicket;
import it.nutrizionista.restnutrizionista.exception.ConflictException;
import it.nutrizionista.restnutrizionista.exception.NotFoundException;
import it.nutrizionista.restnutrizionista.repository.MessaggioAssistenzaRepository;
import it.nutrizionista.restnutrizionista.repository.TicketAssistenzaRepository;
import it.nutrizionista.restnutrizionista.security.AssistenzaRateLimitService;

/** Regole applicative e macchina a stati server-side dell'assistenza. */
@Service
public class AssistenzaService {
    private static final Logger audit = LoggerFactory.getLogger("AUDIT.assistenza");
    private static final Set<StatoTicket> STATI_APERTI = Set.of(StatoTicket.IN_ATTESA, StatoTicket.ACCETTATO);
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_MESSAGGI_POLLING = 200;

    @Autowired private TicketAssistenzaRepository ticketRepository;
    @Autowired private MessaggioAssistenzaRepository messaggioRepository;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private AssistenzaRateLimitService rateLimitService;

    @Transactional
    public TicketAssistenzaDto creaTicket(CreaTicketAssistenzaRequest request) {
        Utente me = currentUserService.getMe();
        rateLimitService.verificaTicket(me.getId());
        if (ticketRepository.existsByNutrizionista_IdAndStatoIn(me.getId(), STATI_APERTI)) {
            throw new ConflictException("Esiste già una richiesta di assistenza aperta");
        }
        TicketAssistenza ticket = new TicketAssistenza();
        ticket.setNutrizionista(me);
        ticket.setOggetto(request.getOggetto().trim());
        ticket.setDescrizione(request.getDescrizione().trim());
        ticket.setStato(StatoTicket.IN_ATTESA);
        ticket = ticketRepository.saveAndFlush(ticket);
        rateLimitService.registraTicket(me.getId());
        audit.info("transizione assistenza utenteId={} ticketId={} NUOVO->IN_ATTESA", me.getId(), ticket.getId());
        return toTicketDto(ticket, me, false);
    }

    @Transactional(readOnly = true)
    public TicketAssistenzaDto ticketAttivo() {
        Utente me = currentUserService.getMe();
        return ticketRepository.findFirstByNutrizionista_IdAndStatoInOrderByCreatedAtDesc(me.getId(), STATI_APERTI)
                .map(ticket -> toTicketDto(ticket, me, false)).orElse(null);
    }

    @Transactional(readOnly = true)
    public TicketAssistenzaDto ticketMio(Long id) {
        Utente me = currentUserService.getMe();
        return toTicketDto(getTicketMio(id, me), me, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketAssistenzaDto> storico(int page, int size) {
        Utente me = currentUserService.getMe();
        Page<TicketAssistenzaDto> risultati = ticketRepository
                .findByNutrizionista_IdOrderByCreatedAtDesc(me.getId(), pagina(page, size, Sort.by("createdAt").descending()))
                .map(ticket -> toTicketDto(ticket, me, false));
        return PageResponse.from(risultati);
    }

    @Transactional
    public TicketAssistenzaDto accetta(Long id) {
        Utente me = currentUserService.getMe();
        TicketAssistenza ticket = getTicketAdmin(id);
        richiediStato(ticket, StatoTicket.IN_ATTESA);
        StatoTicket da = ticket.getStato();
        ticket.setStato(StatoTicket.ACCETTATO);
        ticket.setAcceptedAt(Instant.now());
        ticket = ticketRepository.saveAndFlush(ticket);
        auditTransizione(me, ticket, da, StatoTicket.ACCETTATO);
        return toTicketDto(ticket, me, true);
    }

    @Transactional
    public TicketAssistenzaDto rifiuta(Long id, String motivo) {
        Utente me = currentUserService.getMe();
        TicketAssistenza ticket = getTicketAdmin(id);
        richiediStato(ticket, StatoTicket.IN_ATTESA);
        StatoTicket da = ticket.getStato();
        ticket.setStato(StatoTicket.RIFIUTATO);
        ticket.setMotivoRifiuto(motivo == null || motivo.isBlank() ? null : motivo.trim());
        ticket = ticketRepository.saveAndFlush(ticket);
        auditTransizione(me, ticket, da, StatoTicket.RIFIUTATO);
        return toTicketDto(ticket, me, true);
    }

    @Transactional
    public TicketAssistenzaDto chiudiMio(Long id) {
        Utente me = currentUserService.getMe();
        TicketAssistenza ticket = getTicketMio(id, me);
        if (ticket.getStato() != StatoTicket.IN_ATTESA && ticket.getStato() != StatoTicket.ACCETTATO) {
            throw new ConflictException("Il ticket non può essere chiuso nello stato corrente");
        }
        return chiudi(ticket, me, false);
    }

    @Transactional
    public TicketAssistenzaDto chiudiAdmin(Long id) {
        Utente me = currentUserService.getMe();
        TicketAssistenza ticket = getTicketAdmin(id);
        richiediStato(ticket, StatoTicket.ACCETTATO);
        return chiudi(ticket, me, true);
    }

    private TicketAssistenzaDto chiudi(TicketAssistenza ticket, Utente me, boolean admin) {
        StatoTicket da = ticket.getStato();
        ticket.setStato(StatoTicket.CHIUSO);
        ticket.setClosedAt(Instant.now());
        ticket = ticketRepository.saveAndFlush(ticket);
        auditTransizione(me, ticket, da, StatoTicket.CHIUSO);
        return toTicketDto(ticket, me, admin);
    }

    @Transactional
    public MessaggioAssistenzaDto inviaMessaggioMio(Long ticketId, CreaMessaggioAssistenzaRequest request) {
        Utente me = currentUserService.getMe();
        return salvaMessaggio(getTicketMio(ticketId, me), me, request.getTesto());
    }

    @Transactional
    public MessaggioAssistenzaDto inviaMessaggioAdmin(Long ticketId, CreaMessaggioAssistenzaRequest request) {
        Utente me = currentUserService.getMe();
        return salvaMessaggio(getTicketAdmin(ticketId), me, request.getTesto());
    }

    private MessaggioAssistenzaDto salvaMessaggio(TicketAssistenza ticket, Utente me, String testo) {
        richiediStato(ticket, StatoTicket.ACCETTATO);
        rateLimitService.verificaMessaggio(me.getId());
        MessaggioAssistenza messaggio = new MessaggioAssistenza();
        messaggio.setTicket(ticket);
        messaggio.setMittente(me);
        messaggio.setTesto(testo.trim());
        messaggio = messaggioRepository.saveAndFlush(messaggio);
        rateLimitService.registraMessaggio(me.getId());
        // Deliberatamente non viene mai loggato il testo del messaggio.
        return toMessaggioDto(messaggio, me);
    }

    @Transactional(readOnly = true)
    public PollingMessaggiAssistenzaDto messaggiMiei(Long ticketId, Long afterId) {
        Utente me = currentUserService.getMe();
        return caricaMessaggi(getTicketMio(ticketId, me), me, afterId);
    }

    @Transactional(readOnly = true)
    public PollingMessaggiAssistenzaDto messaggiAdmin(Long ticketId, Long afterId) {
        Utente me = currentUserService.getMe();
        return caricaMessaggi(getTicketAdmin(ticketId), me, afterId);
    }

    private PollingMessaggiAssistenzaDto caricaMessaggi(TicketAssistenza ticket, Utente me, Long afterId) {
        long partenza = afterId == null || afterId < 0 ? 0 : afterId;
        List<MessaggioAssistenzaDto> messaggi = messaggioRepository.findIncrementali(ticket.getId(), partenza,
                PageRequest.of(0, MAX_MESSAGGI_POLLING)).stream()
                .map(messaggio -> toMessaggioDto(messaggio, me)).toList();
        PollingMessaggiAssistenzaDto dto = new PollingMessaggiAssistenzaDto();
        dto.setStato(ticket.getStato());
        dto.setMessaggi(messaggi);
        return dto;
    }

    @Transactional
    public void marcaLettiMio(Long ticketId) {
        Utente me = currentUserService.getMe();
        getTicketMio(ticketId, me);
        messaggioRepository.marcaRicevutiComeLetti(ticketId, me.getId());
    }

    @Transactional
    public void marcaLettiAdmin(Long ticketId) {
        Utente me = currentUserService.getMe();
        getTicketAdmin(ticketId);
        messaggioRepository.marcaRicevutiComeLetti(ticketId, me.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketAssistenzaDto> listaAdmin(StatoTicket stato, int page, int size) {
        Utente me = currentUserService.getMe();
        Page<TicketAssistenzaDto> risultati = ticketRepository
                .findByStatoOrderByCreatedAtAsc(stato, pagina(page, size, Sort.by("createdAt").ascending()))
                .map(ticket -> toTicketDto(ticket, me, true));
        return PageResponse.from(risultati);
    }

    @Transactional(readOnly = true)
    public ConteggiAssistenzaDto conteggi() {
        ConteggiAssistenzaDto dto = new ConteggiAssistenzaDto();
        dto.setInAttesa(ticketRepository.countByStato(StatoTicket.IN_ATTESA));
        dto.setAccettati(ticketRepository.countByStato(StatoTicket.ACCETTATO));
        return dto;
    }

    private TicketAssistenza getTicketMio(Long id, Utente me) {
        TicketAssistenza ticket = getTicketAdmin(id);
        if (!ticket.getNutrizionista().getId().equals(me.getId())) {
            throw new NotFoundException("Ticket di assistenza non trovato");
        }
        return ticket;
    }

    private TicketAssistenza getTicketAdmin(Long id) {
        return ticketRepository.findByIdConNutrizionista(id)
                .orElseThrow(() -> new NotFoundException("Ticket di assistenza non trovato"));
    }

    private void richiediStato(TicketAssistenza ticket, StatoTicket statoRichiesto) {
        if (ticket.getStato() != statoRichiesto) {
            throw new ConflictException("Transizione non consentita dallo stato " + ticket.getStato());
        }
    }

    private PageRequest pagina(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)), sort);
    }

    private void auditTransizione(Utente me, TicketAssistenza ticket, StatoTicket da, StatoTicket a) {
        audit.info("transizione assistenza utenteId={} ticketId={} {}->{}", me.getId(), ticket.getId(), da, a);
    }

    private TicketAssistenzaDto toTicketDto(TicketAssistenza ticket, Utente chiamante, boolean includePersona) {
        TicketAssistenzaDto dto = new TicketAssistenzaDto();
        dto.setId(ticket.getId());
        dto.setOggetto(ticket.getOggetto());
        dto.setDescrizione(ticket.getDescrizione());
        dto.setStato(ticket.getStato());
        dto.setMotivoRifiuto(ticket.getMotivoRifiuto());
        dto.setAcceptedAt(ticket.getAcceptedAt());
        dto.setClosedAt(ticket.getClosedAt());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        dto.setMessaggiNonLetti(messaggioRepository.countByTicket_IdAndLettoFalseAndMittente_IdNot(
                ticket.getId(), chiamante.getId()));
        if (includePersona) {
            Utente utente = ticket.getNutrizionista();
            NutrizionistaAssistenzaDto persona = new NutrizionistaAssistenzaDto();
            persona.setId(utente.getId());
            persona.setNome(utente.getNome());
            persona.setCognome(utente.getCognome());
            persona.setEmail(utente.getEmail());
            dto.setNutrizionista(persona);
        }
        return dto;
    }

    private MessaggioAssistenzaDto toMessaggioDto(MessaggioAssistenza messaggio, Utente chiamante) {
        MessaggioAssistenzaDto dto = new MessaggioAssistenzaDto();
        dto.setId(messaggio.getId());
        dto.setTesto(messaggio.getTesto());
        dto.setLetto(messaggio.isLetto());
        dto.setMioMessaggio(messaggio.getMittente().getId().equals(chiamante.getId()));
        dto.setCreatedAt(messaggio.getCreatedAt());
        return dto;
    }
}
