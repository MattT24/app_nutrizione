package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.PromemoriaDto;
import it.nutrizionista.restnutrizionista.dto.PromemoriaFormDto;
import it.nutrizionista.restnutrizionista.entity.Promemoria;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.PromemoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromemoriaService {

    private final PromemoriaRepository promemoriaRepository;
    private final CurrentUserService currentUserService;

    public PromemoriaService(PromemoriaRepository promemoriaRepository, CurrentUserService currentUserService) {
        this.promemoriaRepository = promemoriaRepository;
        this.currentUserService = currentUserService;
    }

    public PromemoriaDto create(PromemoriaFormDto form) {
        Utente currentUser = currentUserService.getMe();
        Promemoria p = new Promemoria();
        p.setNutrizionista(currentUser);
        mapFormToEntity(form, p);
        
        Promemoria saved = promemoriaRepository.save(p);
        return convertToDto(saved);
    }

    public PromemoriaDto update(Long id, PromemoriaFormDto form) {
        Utente currentUser = currentUserService.getMe();
        Promemoria p = promemoriaRepository.findByIdAndNutrizionista_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promemoria non trovato"));
        
        mapFormToEntity(form, p);
        Promemoria saved = promemoriaRepository.save(p);
        return convertToDto(saved);
    }

    public void delete(Long id) {
        Utente currentUser = currentUserService.getMe();
        Promemoria p = promemoriaRepository.findByIdAndNutrizionista_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promemoria non trovato"));
        
        promemoriaRepository.delete(p);
    }

    public PromemoriaDto getById(Long id) {
        Utente currentUser = currentUserService.getMe();
        Promemoria p = promemoriaRepository.findByIdAndNutrizionista_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promemoria non trovato"));
                
        return convertToDto(p);
    }

    public List<PromemoriaDto> getByDateRange(LocalDate start, LocalDate end) {
        Utente currentUser = currentUserService.getMe();
        List<Promemoria> list = promemoriaRepository.findByNutrizionistaIdAndDateRange(currentUser.getId(), start, end);
        return list.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public PromemoriaDto moveResize(Long id, LocalDateTime start, LocalDateTime end) {
        Utente currentUser = currentUserService.getMe();
        Promemoria p = promemoriaRepository.findByIdAndNutrizionista_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promemoria non trovato"));

        p.setData(start.toLocalDate());
        p.setOra(start.toLocalTime());
        if (end != null) {
            p.setEndData(end.toLocalDate());
            p.setEndOra(end.toLocalTime());
        } else {
            p.setEndData(null);
            p.setEndOra(null);
        }

        Promemoria saved = promemoriaRepository.save(p);
        return convertToDto(saved);
    }

    private void mapFormToEntity(PromemoriaFormDto form, Promemoria p) {
        p.setTesto(form.getTesto());
        p.setData(form.getData());
        p.setOra(form.getOra());
        p.setEndData(form.getEndData());
        p.setEndOra(form.getEndOra());
        p.setAllDay(form.isAllDay());
    }

    private PromemoriaDto convertToDto(Promemoria p) {
        PromemoriaDto dto = new PromemoriaDto();
        dto.setId(p.getId());
        dto.setTesto(p.getTesto());
        dto.setData(p.getData());
        dto.setOra(p.getOra());
        dto.setEndData(p.getEndData());
        dto.setEndOra(p.getEndOra());
        dto.setAllDay(p.isAllDay());
        return dto;
    }
}
