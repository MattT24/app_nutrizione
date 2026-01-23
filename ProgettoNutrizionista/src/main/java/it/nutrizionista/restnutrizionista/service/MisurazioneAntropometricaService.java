package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaDto;
import it.nutrizionista.restnutrizionista.dto.MisurazioneAntropometricaFormDto;
import it.nutrizionista.restnutrizionista.dto.PageResponse;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import jakarta.validation.Valid;

@Service
public class MisurazioneAntropometricaService {

	@Autowired private MisurazioneAntropometricaRepository repo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private UtenteRepository utenteRepo; // Serve per la sicurezza

    private Utente getMe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente corrente non trovato"));
    }

    @Transactional
    public MisurazioneAntropometricaDto create(@Valid MisurazioneAntropometricaFormDto form) {
        Utente me = getMe();
        Cliente cliente = clienteRepo.findByIdAndNutrizionista_Id(form.getCliente().getId(), me.getId())
                .orElseThrow(() -> new RuntimeException("Cliente non trovato o non autorizzato"));

        MisurazioneAntropometrica m = DtoMapper.toMisurazione(form);
        m.setCliente(cliente); // Associo la misurazione al cliente trovato
        
        return DtoMapper.toMisurazioneDtoLight(repo.save(m));
    }


	@Transactional
	public MisurazioneAntropometricaDto update(@Valid MisurazioneAntropometricaFormDto form) {
        Utente me = getMe();
		if (form.getId() == null) throw new RuntimeException("Id Misurazione obbligatoria per update");
		MisurazioneAntropometrica m = repo.findById(form.getId())
				.orElseThrow(() -> new RuntimeException("Misurazione non trovata"));
		if (!m.getCliente().getNutrizionista().getId().equals(me.getId())) {
            throw new RuntimeException("Non autorizzato a modificare questa misurazione");
        }
		DtoMapper.updateMisurazioneFromForm(m, form);
		return DtoMapper.toMisurazioneDtoLight(repo.save(m));
	}

	@Transactional
    public void delete(Long id) { 
		Utente me = getMe();
        MisurazioneAntropometrica m = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Misurazione non trovata"));
        if (!m.getCliente().getNutrizionista().getId().equals(me.getId())) {
            throw new RuntimeException("Non autorizzato a eliminare questa misurazione");
        }
		repo.deleteById(id); }

	@Transactional(readOnly = true)
	public PageResponse<MisurazioneAntropometricaDto> allMisurazioniByCliente(Long id,Pageable pageable) {
		Utente me = getMe();
        boolean isMioCliente = clienteRepo.findByIdAndNutrizionista_Id(id, me.getId()).isPresent();
        if (!isMioCliente) {
            throw new RuntimeException("Cliente non trovato o non autorizzato");
        }
		return PageResponse.from(repo.findByCliente_Id(id,pageable).map(DtoMapper::toMisurazioneDtoLight));
	}

}
