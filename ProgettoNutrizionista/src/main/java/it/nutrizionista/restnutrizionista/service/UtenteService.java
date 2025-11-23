package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.dto.ResetPasswordDto;
import it.nutrizionista.restnutrizionista.dto.UtenteDto;
import it.nutrizionista.restnutrizionista.dto.UtenteFormDto;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

/** Logica di business per Utenti. */
@Service
public class UtenteService {

    @Autowired private UtenteRepository repo;
    @Autowired private RuoloRepository ruoloRepo;
    @Autowired private PasswordEncoder encoder;

    /*lolo*/

    @Transactional
    public UtenteDto create(UtenteFormDto form) {
        Utente u = new Utente();
        apply(u, form, true);
        return DtoMapper.toUtenteDto(repo.save(u)); // Ruolo light dentro UtenteDto
    }

    /** Aggiorna utente. */
    @Transactional
    public UtenteDto update(UtenteFormDto form) {
        if (form.getId() == null) throw new RuntimeException("Id utente obbligatorio per update");
        Utente u = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        apply(u, form, false);
        return DtoMapper.toUtenteDto(repo.save(u)); // Ruolo light
    }
    /** Aggiorna Profilo Utente. */
    @Transactional
    public UtenteDto updateProfile(UtenteFormDto form) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utente ut = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
		if (ut.getId() != form.getId()) throw new RuntimeException("L'utente non è il proprietario dell'account");
        if (form.getId() == null) throw new RuntimeException("Id utente obbligatorio per update");
        Utente u = repo.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        apply(u, form, false);
        return DtoMapper.toUtenteDto(repo.save(u)); // Ruolo light
    }

    /** Elimina utente. */
    @Transactional
    public void delete(Long id) { repo.deleteById(id); }
    
    public void deleteMyProfile() {
	    	String email = SecurityContextHolder.getContext().getAuthentication().getName();
	    	Utente u = repo.findByEmail(email)
	    			.orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
	    	repo.delete(u);
    }
    
    /** Dettaglio utente. (Ruolo light) */
    @Transactional(readOnly = true)
    public UtenteDto getById(Long id) {
        return repo.findById(id)
                .map(DtoMapper::toUtenteDto)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
    }

    /** Lista paginata utenti (mapper light). */
    @Transactional(readOnly = true)
    public PageResponse<UtenteDto> list(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable).map(DtoMapper::toUtenteDto));
    }

    /** Lista completa non paginata (mapper light). */
    @Transactional(readOnly = true)
    public List<UtenteDto> listAll() {
        return repo.findAll().stream()
                .map(DtoMapper::toUtenteDto)
                .collect(Collectors.toList());
    }

    /** Profilo utente corrente (ricavato dal JWT). */
    @Transactional(readOnly = true)
    public UtenteDto getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utente u = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
        return DtoMapper.toUtenteDto(u);
    }
    /** Copia campi dal form, gestendo password e ruolo. */
    private void apply(Utente u, UtenteFormDto form, boolean encodePasswordAlways) {
        u.setNome(form.getNome());
        u.setCognome(form.getCognome());
        u.setCodiceFiscale(form.getCodiceFiscale());
        u.setEmail(form.getEmail());
        if (encodePasswordAlways || (form.getPassword() != null && !form.getPassword().isBlank())) {
            u.setPassword(encoder.encode(form.getPassword()));
        }
        u.setDataNascita(form.getDataNascita());
        u.setTelefono(form.getTelefono());
        u.setIndirizzo(form.getIndirizzo());

        Ruolo ruolo = ruoloRepo.findById(form.getRuolo().getId())
                .orElseThrow(() -> new RuntimeException("Ruolo non trovato"));
        u.setRuolo(ruolo);
    }
    
    public UtenteDto updateMyProfile(UtenteFormDto form) {
    	// controllo token
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utente u = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
        
        u.setNome(form.getNome());
        u.setCognome(form.getCognome());
        u.setCodiceFiscale(form.getCodiceFiscale());
        u.setEmail(form.getEmail());
        u.setDataNascita(form.getDataNascita());
        u.setTelefono(form.getTelefono());
        u.setIndirizzo(form.getIndirizzo());
        
        return DtoMapper.toUtenteDto(repo.save(u));
    }
    
    public UtenteDto updateMyPassword(@Valid ResetPasswordDto dto) {    	
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Utente utente = repo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Utente non trovato con email: " + dto.getEmail()));
       
        if (!dto.getPassword().equals(dto.getConfermaPassword())) {
            throw new RuntimeException("Le password inserite non coincidono");
        }
       
        utente.setPassword(encoder.encode(dto.getPassword()));     
        Utente updated = repo.save(utente);
        return DtoMapper.toUtenteDto(updated);
    }
}
