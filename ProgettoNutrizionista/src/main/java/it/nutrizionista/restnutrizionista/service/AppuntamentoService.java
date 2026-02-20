package it.nutrizionista.restnutrizionista.service;

import java.time.Duration;
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

        normalizeFormDates(form, null);
        validateBusiness(form, me.getId(), null);

        Appuntamento a = DtoMapper.toAppuntamento(form, me, cliente);

        // ✅ safety: nel caso il mapper non gestisca i nuovi campi, li imposto comunque qui
        a.setEndData(form.getEndData());
        a.setEndOra(form.getEndOra());
        a.setTimezone(form.getTimezone());
        a.setAllDay(Boolean.TRUE.equals(form.getAllDay()));

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

        normalizeFormDates(form, a);
        validateBusiness(form, me.getId(), id);

        DtoMapper.updateAppuntamentoFromFormDto(a, form);

        // ✅ safety: nel caso il mapper non gestisca i nuovi campi, li imposto comunque qui
        a.setEndData(form.getEndData());
        a.setEndOra(form.getEndOra());
        a.setTimezone(form.getTimezone());
        a.setAllDay(Boolean.TRUE.equals(form.getAllDay()));

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

        // FullCalendar spesso passa end esclusivo
        LocalDate inclusiveEnd = end.minusDays(1);

        // ✅ include eventi che “overlappano” il range, anche se iniziano prima
        return repo.findByNutrizionista_IdAndDataLessThanEqualAndEndDataGreaterThanEqual(me.getId(), inclusiveEnd, start).stream()
                .map(this::toCalendarEvent)
                .toList();
    }

    @Transactional
    public AppuntamentoDto moveResize(Long id, LocalDateTime newStart, LocalDateTime newEnd) {
        Utente me = getMe();
        Appuntamento a = ownershipValidator.getOwnedAppuntamento(id);

        // durata esistente (fallback 60 se dati vecchi senza end)
        LocalDateTime oldStart = LocalDateTime.of(a.getData(), a.getOra());
        LocalDateTime oldEnd = getSafeEnd(a);

        Duration dur = Duration.between(oldStart, oldEnd);
        if (dur.isNegative() || dur.isZero()) dur = Duration.ofMinutes(60);

        LocalDateTime computedEnd = (newEnd != null) ? newEnd : newStart.plus(dur);

        AppuntamentoFormDto form = new AppuntamentoFormDto();
        form.setData(newStart.toLocalDate());
        form.setOra(newStart.toLocalTime());
        form.setEndData(computedEnd.toLocalDate());
        form.setEndOra(computedEnd.toLocalTime());

        form.setTimezone(a.getTimezone() != null ? a.getTimezone() : "Europe/Rome");
        form.setAllDay(a.isAllDay());

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
        a.setEndData(form.getEndData());
        a.setEndOra(form.getEndOra());
        a.setTimezone(form.getTimezone());
        a.setAllDay(Boolean.TRUE.equals(form.getAllDay()));

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

    private void normalizeFormDates(AppuntamentoFormDto form, Appuntamento existing) {
        if (form.getTimezone() == null || form.getTimezone().trim().isEmpty()) {
            form.setTimezone(existing != null && existing.getTimezone() != null ? existing.getTimezone() : "Europe/Rome");
        }

        if (form.getAllDay() == null) {
            form.setAllDay(existing != null ? existing.isAllDay() : false);
        }

        // end mancante -> default: start + 60 minuti
        if (form.getEndData() == null || form.getEndOra() == null) {
            if (form.getData() != null && form.getOra() != null) {
                LocalDateTime start = LocalDateTime.of(form.getData(), form.getOra());

                // se stiamo aggiornando e l'esistente ha end valido, mantengo la durata
                if (existing != null && existing.getData() != null && existing.getOra() != null) {
                    LocalDateTime oldStart = LocalDateTime.of(existing.getData(), existing.getOra());
                    LocalDateTime oldEnd = getSafeEnd(existing);

                    Duration dur = Duration.between(oldStart, oldEnd);
                    if (!dur.isNegative() && !dur.isZero()) {
                        LocalDateTime end = start.plus(dur);
                        form.setEndData(end.toLocalDate());
                        form.setEndOra(end.toLocalTime());
                        return;
                    }
                }

                LocalDateTime end = start.plusMinutes(60);
                form.setEndData(end.toLocalDate());
                form.setEndOra(end.toLocalTime());
            }
        }
    }

    private LocalDateTime getSafeEnd(Appuntamento a) {
        if (a.getEndData() != null && a.getEndOra() != null) {
            return LocalDateTime.of(a.getEndData(), a.getEndOra());
        }
        LocalDateTime start = LocalDateTime.of(a.getData(), a.getOra());
        return start.plusMinutes(60);
    }

    private void validateBusiness(AppuntamentoFormDto form, Long nutrizionistaId, Long excludeId) {
        if (form.getData() == null) throw new RuntimeException("Data obbligatoria");
        if (form.getOra() == null) throw new RuntimeException("Ora obbligatoria");
        if (isBlank(form.getDescrizioneAppuntamento())) throw new RuntimeException("Descrizione obbligatoria");
        if (form.getModalita() == null) throw new RuntimeException("Modalità obbligatoria");

        // ✅ end obbligatorio “a valle” della normalizzazione
        if (form.getEndData() == null) throw new RuntimeException("Data fine obbligatoria");
        if (form.getEndOra() == null) throw new RuntimeException("Ora fine obbligatoria");

        LocalDateTime start = LocalDateTime.of(form.getData(), form.getOra());
        LocalDateTime end = LocalDateTime.of(form.getEndData(), form.getEndOra());

        if (!end.isAfter(start)) {
            throw new RuntimeException("L'orario di fine deve essere dopo l'orario di inizio");
        }

        // slot lavorativo (come già fai) – qui lo tengo semplice
        LocalTime apertura = LocalTime.of(8, 0);
        LocalTime chiusura = LocalTime.of(20, 0);
        if (form.getOra().isBefore(apertura) || form.getOra().isAfter(chiusura)) {
            throw new RuntimeException("L'appuntamento deve essere tra le 8:00 e le 20:00");
        }

        // ✅ conflitto su intervallo (overlap), update-safe
        // prendo candidati che overlappano per date (grezzo) e poi check preciso con LocalDateTime
        List<Appuntamento> candidates = repo.findByNutrizionista_IdAndDataLessThanEqualAndEndDataGreaterThanEqual(
                nutrizionistaId,
                form.getEndData(),
                form.getData()
        );

        for (Appuntamento a : candidates) {
            if (excludeId != null && a.getId() != null && a.getId().equals(excludeId)) continue;

            LocalDateTime aStart = LocalDateTime.of(a.getData(), a.getOra());
            LocalDateTime aEnd = getSafeEnd(a);

            // overlap standard: existingStart < newEnd AND existingEnd > newStart  (end esclusivo)
            boolean overlap = aStart.isBefore(end) && aEnd.isAfter(start);
            if (overlap) throw new RuntimeException("Hai già un appuntamento in questa fascia oraria");
        }

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

        // ✅ end persistito (fallback 60 min per dati vecchi)
        LocalDateTime end = getSafeEnd(a);
        ev.setEnd(end);

        var props = new HashMap<String, Object>();
        props.put("stato", a.getStato());
        props.put("modalita", a.getModalita());
        props.put("luogo", a.getLuogo());
        props.put("emailCliente", a.getEmailCliente());
        props.put("descrizione", a.getDescrizioneAppuntamento());
        props.put("clienteRegistrato", a.getCliente() != null);
        props.put("clienteId", a.getCliente() != null ? a.getCliente().getId() : null);

        // ✅ nuovi campi utili lato UI
        props.put("timezone", a.getTimezone());
        props.put("allDay", a.isAllDay());

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