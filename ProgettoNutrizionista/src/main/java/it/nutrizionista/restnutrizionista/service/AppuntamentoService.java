package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.*;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.OrariStudioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppuntamentoService {

    private final AppuntamentoRepository appuntamentoRepository;
    private final OrariStudioRepository orariStudioRepository;
    private final ClienteRepository clienteRepository;
    private final CurrentUserService currentUserService;

    public AppuntamentoService(
            AppuntamentoRepository appuntamentoRepository,
            OrariStudioRepository orariStudioRepository,
            ClienteRepository clienteRepository,
            CurrentUserService currentUserService) {
        this.appuntamentoRepository = appuntamentoRepository;
        this.orariStudioRepository = orariStudioRepository;
        this.clienteRepository = clienteRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AppuntamentoDto create(AppuntamentoFormDto form) {
        Utente nutrizionista = currentUserService.getMe();
        
        Appuntamento appuntamento = new Appuntamento();
        appuntamento.setNutrizionista(nutrizionista);
        
        mapFormToEntity(form, appuntamento);
        validaSlotOrario(appuntamento);
        
        if (appuntamento.getStato() == null) {
            appuntamento.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
        }
        
        Appuntamento salvato = appuntamentoRepository.save(appuntamento);
        return convertToDto(salvato);
    }

    @Transactional
    public AppuntamentoDto update(Long id, AppuntamentoFormDto form) {
        Appuntamento esistente = appuntamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appuntamento non trovato"));
        
        Utente nutrizionista = currentUserService.getMe();
        if (!esistente.getNutrizionista().getId().equals(nutrizionista.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non hai i permessi per modificare questo appuntamento");
        }

        mapFormToEntity(form, esistente);
        validaSlotOrario(esistente);
        
        Appuntamento salvato = appuntamentoRepository.save(esistente);
        return convertToDto(salvato);
    }

    public AppuntamentoDto getById(Long id) {
        Appuntamento appuntamento = appuntamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appuntamento non trovato"));
        return convertToDto(appuntamento);
    }

    @Transactional
    public void delete(Long id) {
        Appuntamento esistente = appuntamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appuntamento non trovato"));
        
        // Soft delete per storico
        esistente.setStato(Appuntamento.StatoAppuntamento.ANNULLATO);
        appuntamentoRepository.save(esistente);
    }

    public List<AppuntamentoDto> getUpcoming() {
        Utente nutrizionista = currentUserService.getMe();
        return appuntamentoRepository.findTop4ByNutrizionistaIdAndDataGreaterThanEqualOrderByDataAscOraAsc(
                nutrizionista.getId(), LocalDate.now())
                .stream()
                .filter(a -> a.getStato() != Appuntamento.StatoAppuntamento.ANNULLATO)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Interfaccia Angular Calendar / Full Calendar: ritorna gli eventi di "/me" formattati
     */
    public List<CalendarEventDto> getMyCalendarEvents(LocalDate start, LocalDate end) {
        Utente nutrizionista = currentUserService.getMe();
        List<Appuntamento> appuntamenti = appuntamentoRepository.findByNutrizionistaIdAndDateRange(
                nutrizionista.getId(), start, end);
        
        List<CalendarEventDto> eventi = new ArrayList<>();
        
        for (Appuntamento app : appuntamenti) {
            if (app.getStato() == Appuntamento.StatoAppuntamento.ANNULLATO) {
                continue; // Nascondiamo gli appuntamenti annullati dalla griglia
            }
            
            CalendarEventDto event = new CalendarEventDto();
            event.setId(app.getId());
            event.setAllDay(app.isAllDay());
            
            // Compone il titolo
            // Compone il titolo: priorità al nome manuale (se presente), altrimenti al cliente registrato
            String manualName = (app.getClienteNome() != null && !app.getClienteNome().isBlank()) 
                    ? app.getClienteNome() + " " + app.getClienteCognome() 
                    : null;
            
            String nomeCliente = manualName != null 
                    ? manualName 
                    : (app.getCliente() != null ? app.getCliente().getNome() + " " + app.getCliente().getCognome() : "Anonimo");
            
            event.setTitle(nomeCliente + (app.getModalita() != null ? " - " + app.getModalita() : ""));
            
            // Imposta start e end formattati ISO per Angular
            if (app.isAllDay()) {
                event.setStart(app.getData().atStartOfDay());
                event.setEnd(app.getEndData().atTime(LocalTime.MAX));
            } else {
                event.setStart(LocalDateTime.of(app.getData(), app.getOra() != null ? app.getOra() : LocalTime.MIDNIGHT));
                event.setEnd(LocalDateTime.of(app.getEndData(), app.getEndOra() != null ? app.getEndOra() : LocalTime.MAX));
            }
            
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", app.getId());
            meta.put("stato", app.getStato() != null ? app.getStato().name() : "PRENOTATO");
            meta.put("modalita", app.getModalita() != null ? app.getModalita().name() : "");
            meta.put("clienteId", app.getCliente() != null ? app.getCliente().getId() : null);
            meta.put("clienteNome", app.getCliente() != null ? app.getCliente().getNome() : app.getClienteNome());
            meta.put("clienteCognome", app.getCliente() != null ? app.getCliente().getCognome() : app.getClienteCognome());
            meta.put("descrizioneAppuntamento", app.getDescrizioneAppuntamento());
            meta.put("emailCliente", app.getEmailCliente());
            meta.put("isPast", event.getEnd().isBefore(LocalDateTime.now()));
            event.setMeta(meta);
            
            eventi.add(event);
        }
        return eventi;
    }

    /**
     * Metodo scatenato dal Drag & Drop / Resize di Angular Calendar
     */
    @Transactional
    public AppuntamentoDto moveResize(Long id, LocalDateTime start, LocalDateTime end) {
        Appuntamento esistente = appuntamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appuntamento non trovato"));
        
        esistente.setData(start.toLocalDate());
        esistente.setOra(start.toLocalTime());
        
        if (end != null) {
            esistente.setEndData(end.toLocalDate());
            esistente.setEndOra(end.toLocalTime());
        } else {
            // Se trascini un evento, la library potrebbe non inviare l'end-time, manteniamo la vecchia durata (es 1 ora)
            esistente.setEndData(start.toLocalDate());
            esistente.setEndOra(start.toLocalTime().plusHours(1));
        }

        validaSlotOrario(esistente);
        Appuntamento salvato = appuntamentoRepository.save(esistente);
        return convertToDto(salvato);
    }

    public List<ClienteDropdownDto> searchMyClientsForDropdown(String q) {
        Utente nutrizionista = currentUserService.getMe(); 
        
        // Utilizziamo la TUA query personalizzata per fare la ricerca direttamente nel DB
        return clienteRepository.searchMyClientsByName(nutrizionista.getId(), q)
                .stream()
                .map(c -> {
                    ClienteDropdownDto dto = new ClienteDropdownDto();
                    dto.setId(c.getId());
                    dto.setNome(c.getNome()); 
                    dto.setCognome(c.getCognome());
                    dto.setEmail(c.getEmail()); // <-- FIX: email era mancante
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ==========================================
    // METODI PRIVATI DI VALIDAZIONE E MAPPATURA
    // ==========================================

    private void validaSlotOrario(Appuntamento nuovoApp) {
        if (nuovoApp.isAllDay() || nuovoApp.getOra() == null || nuovoApp.getEndOra() == null) return;

        LocalDate endData = nuovoApp.getEndData() != null ? nuovoApp.getEndData() : nuovoApp.getData();
        LocalDateTime inizioNuovo = LocalDateTime.of(nuovoApp.getData(), nuovoApp.getOra());
        LocalDateTime fineNuovo = LocalDateTime.of(endData, nuovoApp.getEndOra());

        if (!inizioNuovo.isBefore(fineNuovo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'inizio deve essere antecedente alla fine.");
        }

        // CONTROLLO ORARI STUDIO IN BASE AL GIORNO DELLA SETTIMANA
        DayOfWeek giorno = inizioNuovo.getDayOfWeek();
        OrariStudio orario = orariStudioRepository.findByNutrizionistaIdAndGiornoSettimana(nuovoApp.getNutrizionista().getId(), giorno).orElse(null);
        
        if (orario != null) {
            if (!orario.isGiornoLavorativo()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lo studio è chiuso di " + giorno);
            }
            if (orario.getOraApertura() != null && orario.getOraChiusura() != null) {
                if (nuovoApp.getOra().isBefore(orario.getOraApertura()) || nuovoApp.getEndOra().isAfter(orario.getOraChiusura())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fuori orario lavorativo.");
                }
            }
            if (orario.getInizioPausaPranzo() != null && orario.getFinePausaPranzo() != null) {
                boolean sovrapponePausa = nuovoApp.getOra().isBefore(orario.getFinePausaPranzo()) && nuovoApp.getEndOra().isAfter(orario.getInizioPausaPranzo());
                if (sovrapponePausa) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sovrapposizione con pausa pranzo.");
                }
            }
        }

        // Controllo Sovrapposizioni (Overbooking)
        List<Appuntamento> esistenti = appuntamentoRepository.findByNutrizionistaIdAndDateRange(
                nuovoApp.getNutrizionista().getId(), nuovoApp.getData().minusDays(1), endData.plusDays(1)
        );

        for (Appuntamento esistente : esistenti) {
            if (esistente.getId() != null && esistente.getId().equals(nuovoApp.getId())) continue;
            if (esistente.getStato() == Appuntamento.StatoAppuntamento.ANNULLATO) continue;
            if (esistente.isAllDay() || esistente.getOra() == null || esistente.getEndOra() == null) continue;

            LocalDate existEndData = esistente.getEndData() != null ? esistente.getEndData() : esistente.getData();
            LocalDateTime inizioEsistente = LocalDateTime.of(esistente.getData(), esistente.getOra());
            LocalDateTime fineEsistente = LocalDateTime.of(existEndData, esistente.getEndOra());

            if (inizioNuovo.isBefore(fineEsistente) && fineNuovo.isAfter(inizioEsistente)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lo slot orario risulta già occupato da un altro appuntamento.");
            }
        }
    }

    private void mapFormToEntity(AppuntamentoFormDto form, Appuntamento app) {
        app.setData(form.getData());
        app.setOra(form.getOra());
        app.setEndData(form.getEndData());
        app.setEndOra(form.getEndOra());
        app.setTimezone(form.getTimezone());
        app.setAllDay(form.isAllDay());
        app.setModalita(form.getModalita());
        app.setStato(form.getStato());
        app.setLuogo(form.getLuogo());
        app.setDescrizioneAppuntamento(form.getDescrizioneAppuntamento());
        
        app.setClienteRegistrato(form.isClienteRegistrato());
        app.setClienteNome(form.getClienteNome());
        app.setClienteCognome(form.getClienteCognome());

        if (form.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(form.getClienteId()).orElse(null);
            app.setCliente(cliente);
            // FIX: se l'email non arriva dal form, la prendiamo dall'entità cliente
            if (form.getEmailCliente() != null && !form.getEmailCliente().isBlank()) {
                app.setEmailCliente(form.getEmailCliente());
            } else if (cliente != null) {
                app.setEmailCliente(cliente.getEmail());
            } else {
                app.setEmailCliente(form.getEmailCliente());
            }
        } else {
            app.setCliente(null);
            app.setEmailCliente(form.getEmailCliente());
        }
    }

    private AppuntamentoDto convertToDto(Appuntamento app) {
        AppuntamentoDto dto = new AppuntamentoDto();
        dto.setId(app.getId());
        
        if (app.getNutrizionista() != null) {
            dto.setNutrizionistaId(app.getNutrizionista().getId());
            dto.setNutrizionistaNome(app.getNutrizionista().getNome());
            dto.setNutrizionistaCognome(app.getNutrizionista().getCognome());
        }
        
        if (app.getCliente() != null) {
            dto.setClienteId(app.getCliente().getId());
        }
        
        dto.setClienteNome(app.getClienteNome());
        dto.setClienteCognome(app.getClienteCognome());
        dto.setClienteRegistrato(app.isClienteRegistrato());
        dto.setDescrizioneAppuntamento(app.getDescrizioneAppuntamento());
        dto.setData(app.getData());
        dto.setOra(app.getOra());
        dto.setEndData(app.getEndData());
        dto.setEndOra(app.getEndOra());
        dto.setTimezone(app.getTimezone());
        dto.setAllDay(app.isAllDay());
        dto.setModalita(app.getModalita());
        dto.setStato(app.getStato());
        dto.setLuogo(app.getLuogo());
        dto.setEmailCliente(app.getEmailCliente());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        
        return dto;
    }
}