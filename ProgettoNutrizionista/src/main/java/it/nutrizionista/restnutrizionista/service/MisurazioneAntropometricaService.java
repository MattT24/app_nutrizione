package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
import jakarta.validation.Valid;

@Service
public class MisurazioneAntropometricaService {

	@Autowired private MisurazioneAntropometricaRepository repo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private CurrentUserService currentUserService;
    @Autowired private OwnershipValidator ownershipValidator;

    @Transactional
    public MisurazioneAntropometricaDto create(@Valid MisurazioneAntropometricaFormDto form) {
        Cliente cliente = ownershipValidator.getOwnedCliente(form.getCliente().getId());

        MisurazioneAntropometrica m = DtoMapper.toMisurazione(form);
        m.setCliente(cliente); // Associo la misurazione al cliente trovato
        
        return DtoMapper.toMisurazioneDtoLight(repo.save(m));
    }


	@Transactional
	public MisurazioneAntropometricaDto update(@Valid MisurazioneAntropometricaFormDto form) {
		if (form.getId() == null) throw new RuntimeException("Id Misurazione obbligatoria per update");
		MisurazioneAntropometrica m = ownershipValidator.getOwnedMisurazioneAntropometrica(form.getId());
		DtoMapper.updateMisurazioneFromForm(m, form);
		return DtoMapper.toMisurazioneDtoLight(repo.save(m));
	}

	@Transactional
    public void delete(Long id) { 
        MisurazioneAntropometrica m = ownershipValidator.getOwnedMisurazioneAntropometrica(id);
		repo.deleteById(id); }

	@Transactional(readOnly = true)
	public PageResponse<MisurazioneAntropometricaDto> allMisurazioniCliente(Long id,Pageable pageable) {
        ownershipValidator.getOwnedCliente(id);
		return PageResponse.from(repo.findByCliente_IdOrderByDataMisurazioneDesc(id,pageable).map(DtoMapper::toMisurazioneDtoLight));
	}

}
