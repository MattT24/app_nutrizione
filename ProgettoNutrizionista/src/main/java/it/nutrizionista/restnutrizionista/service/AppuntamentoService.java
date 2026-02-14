package it.nutrizionista.restnutrizionista.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.AppuntamentoDto;
import it.nutrizionista.restnutrizionista.dto.AppuntamentoFormDto;
import it.nutrizionista.restnutrizionista.dto.CalendarEventDto;
import it.nutrizionista.restnutrizionista.dto.ClienteDropdownDto;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;

@Service
public class AppuntamentoService {

    private final AppuntamentoRepository repo;
    private final ClienteRepository repoCliente;
    
    @Autowired private CurrentUserService currentUserService;
    @Autowired private OwnershipValidator ownershipValidator;

    public AppuntamentoService(AppuntamentoRepository repo, ClienteRepository repoCliente) {
        this.repo = repo;
        this.repoCliente = repoCliente;
    }

    private Utente getMe() {
        return currentUserService.getMe();
    }

    // ============ CRUD ============

    @Transactional
    public AppuntamentoDto create(AppuntamentoFormDto form) {
        Utente me = getMe();

        Cliente cliente = resolveCliente(form);

        validateBusiness(form, me.getId(), null);

        Appuntamento a = DtoMapper.toAppuntamento(form, me, cliente);
        Appuntamento saved = repo.save(a);
        return DtoMapper.toAppuntamentoDto(saved);
    }

    @Transactional
    public AppuntamentoDto update(Long id, AppuntamentoFormDto form) {
        Utente me = getMe();
        Appuntamento a = ownershipValidator.getOwnedAppuntamento(id);

        Cliente cliente = resolveCliente(form);

        // se passo cliente registrato, pulisco i campi temp; se non registrato, setto cliente null
        a.setCliente(cliente);
        if (cliente != null) {
            a.setClienteNomeTemp(null);
            a.setClienteCognomeTemp(null);
        }

        validateBusiness(form, me.getId(), id);

        DtoMapper.updateAppuntamentoFromFormDto(a, form);
        Appuntamento saved = repo.save(a);
        return DtoMapper.toAppuntamentoDto(saved);
    }

    @Transactional(readOnly = true)
    public AppuntamentoDto getById(Long id) {
        Appuntamento a = ownershipValidator.getOwnedAppuntamento(id);
        return DtoMapper.toAppuntamentoDto(a);
    }

    @Transactional
    public void delete(Long id) {
        Appuntamento a = ownershipValidator.getOwnedAppuntamento(id);

        // regola opzionale: non cancellare confermati
        if (a.getStato() == Appuntamento.StatoAppuntamento.CONFERMATO) {
            throw new RuntimeException("Non è possibile eliminare un appuntamento confermato. Annullalo prima.");
        }
        repo.delete(a);
    }

    // ============ FULLCALENDAR: GET range + drag/drop ============

    @Transactional(readOnly = true)
    public List<CalendarEventDto> getMyCalendarEvents(LocalDate start, LocalDate end) {
        Utente me = getMe();

        // FullCalendar spesso passa end esclusivo; per sicurezza includiamo anche il giorno precedente:
        LocalDate inclusiveEnd = end.minusDays(1);

        return repo.findByNutrizionista_IdAndDataBetween(me.getId(), start, inclusiveEnd).stream()
                .map(this::toCalendarEvent)
                .toList();
    }

    @Transactional
    public AppuntamentoDto moveResize(Long id, LocalDateTime newStart, LocalDateTime newEnd) {
        Utente me = getMe();
        Appuntamento a = ownershipValidator.getOwnedAppuntamento(id);

        // per ora usiamo solo start -> data/ora
        AppuntamentoFormDto form = new AppuntamentoFormDto();
        form.setData(newStart.toLocalDate());
        form.setOra(newStart.toLocalTime());
        form.setDescrizioneAppuntamento(a.getDescrizioneAppuntamento());
        form.setModalita(a.getModalita());
        form.setStato(a.getStato());
        form.setLuogo(a.getLuogo());
        form.setEmailCliente(a.getEmailCliente());

        if (a.getCliente() != null) {
            form.setClienteId(a.getCliente().getId());
        } else {
            form.setClienteNome(a.getClienteNomeTemp());
            form.setClienteCognome(a.getClienteCognomeTemp());
        }

        validateBusiness(form, me.getId(), id);

        a.setData(form.getData());
        a.setOra(form.getOra());

        return DtoMapper.toAppuntamentoDto(repo.save(a));
    }

    // ============ Helpers ============

    private Cliente resolveCliente(AppuntamentoFormDto form) {
        if (form.getClienteId() != null) {
        	return ownershipValidator.getOwnedCliente(form.getClienteId());
        }

        // cliente non registrato: obbligatori
        if (isBlank(form.getClienteNome())) throw new RuntimeException("Il nome del cliente è obbligatorio");
        if (isBlank(form.getClienteCognome())) throw new RuntimeException("Il cognome del cliente è obbligatorio");
        if (isBlank(form.getEmailCliente())) throw new RuntimeException("L'email del cliente è obbligatoria");

        return null;
    }

    private void validateBusiness(AppuntamentoFormDto form, Long nutrizionistaId, Long excludeId) {
        if (form.getData() == null) throw new RuntimeException("Data obbligatoria");
        if (form.getOra() == null) throw new RuntimeException("Ora obbligatoria");
        if (isBlank(form.getDescrizioneAppuntamento())) throw new RuntimeException("Descrizione obbligatoria");
        if (form.getModalita() == null) throw new RuntimeException("Modalità obbligatoria");

        // slot lavorativo (come già fai) – qui lo tengo semplice
        LocalTime apertura = LocalTime.of(8, 0);
        LocalTime chiusura = LocalTime.of(20, 0);
        if (form.getOra().isBefore(apertura) || form.getOra().isAfter(chiusura)) {
            throw new RuntimeException("L'appuntamento deve essere tra le 8:00 e le 20:00");
        }

        // conflitto nutrizionista (update-safe)
        boolean busy = (excludeId == null)
                ? repo.existsByNutrizionista_IdAndDataAndOra(nutrizionistaId, form.getData(), form.getOra())
                : repo.existsByNutrizionista_IdAndDataAndOraAndIdNot(nutrizionistaId, form.getData(), form.getOra(), excludeId);

        if (busy) throw new RuntimeException("Hai già un appuntamento in questa data e ora");

        // in presenza -> luogo obbligatorio
        if (form.getModalita() == Appuntamento.Modalita.IN_PRESENZA && isBlank(form.getLuogo())) {
            throw new RuntimeException("Il luogo è obbligatorio per gli appuntamenti in presenza");
        }
    }

    private CalendarEventDto toCalendarEvent(Appuntamento a) {
        CalendarEventDto ev = new CalendarEventDto();
        ev.setId(a.getId());

        String clienteNome = a.getCliente() != null ? a.getCliente().getNome() : a.getClienteNomeTemp();
        String clienteCognome = a.getCliente() != null ? a.getCliente().getCognome() : a.getClienteCognomeTemp();

        ev.setTitle((clienteNome != null ? clienteNome : "") + " " + (clienteCognome != null ? clienteCognome : ""));

        LocalDateTime start = LocalDateTime.of(a.getData(), a.getOra());
        ev.setStart(start);
        ev.setEnd(start.plusMinutes(60)); // durata base 60 min (poi possiamo renderla configurabile)

        var props = new HashMap<String, Object>();
        props.put("stato", a.getStato());
        props.put("modalita", a.getModalita());
        props.put("luogo", a.getLuogo());
        props.put("emailCliente", a.getEmailCliente());
        props.put("descrizione", a.getDescrizioneAppuntamento());
        props.put("clienteRegistrato", a.getCliente() != null);
        props.put("clienteId", a.getCliente() != null ? a.getCliente().getId() : null);

        ev.setExtendedProps(props);
        return ev;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    @Transactional(readOnly = true)
    public List<ClienteDropdownDto> searchMyClientsForDropdown(String q) {
        Utente me = getMe();

        String query = (q == null) ? "" : q.trim();
        if (query.isEmpty()) {
            return List.of(); // evita di caricare tutti i clienti senza filtro
        }

        return repoCliente.searchMyClientsByName(me.getId(), query).stream()
                .limit(10) // limita risultati per dropdown
                .map(c -> new ClienteDropdownDto(
                        c.getId(),
                        c.getNome(),
                        c.getCognome(),
                        c.getDataNascita(),
                        c.getEmail()
                ))
                .toList();
    }

}
